package com.lytoyo.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lytoyo.common.domain.FileInfo;
import com.lytoyo.common.domain.Message;
import com.lytoyo.common.domain.Result;
import com.lytoyo.common.domain.User;
import com.lytoyo.common.domain.vo.ContentVo;
import com.lytoyo.common.domain.vo.MessageVo;
import com.lytoyo.common.domain.vo.UserVo;
import com.lytoyo.common.properties.MinioProperties;
import com.lytoyo.common.utils.AuthContextHolder;
import com.lytoyo.common.utils.WebsocketUtil;
import com.lytoyo.mapper.MessageMapper;
import com.lytoyo.mapper.UserMapper;
import com.lytoyo.service.FileService;
import com.lytoyo.service.MessageService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Package:com.lytoyo.service.impl
 *
 * @ClassName:MessageServiceImpl
 * @Create:2026/1/6 15:42
 **/
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {
    @Resource
    private MessageMapper messageMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private MinioProperties minioProperties;

    @Resource
    private MinioClient minioClient;

    @Resource
    private FileService fileService;

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    @Override
    public Result checkP2pId(Long otherId) {
        Long userId = AuthContextHolder.getUserId();
        List<Message> aP2pMessageList = this.list(new LambdaQueryWrapper<Message>().eq(Message::getFromUserId, userId)
                .eq(Message::getToUserId, otherId));

        List<Message> bP2pMessageList = this.list(new LambdaQueryWrapper<Message>().eq(Message::getToUserId, userId)
                .eq(Message::getFromUserId, otherId));
        Message aP2pMessage = null;
        Message bP2pMessage = null;
        if (aP2pMessageList.size() > 0) aP2pMessage = aP2pMessageList.stream().findFirst().get();
        if (bP2pMessageList.size() > 0) bP2pMessage = bP2pMessageList.stream().findFirst().get();
        if (null == aP2pMessage && null == bP2pMessage) return Result.success(-1);
        Long p2pId = aP2pMessage == null ? bP2pMessage.getP2pId() : aP2pMessage.getP2pId();
        return Result.success(p2pId);
    }

    @Override
    @Transactional
    public Result createPrivateWindow(Message message) {
        Long userId = AuthContextHolder.getUserId();
        //判断是否已经存在P2pId
        Result result = checkP2pId(message.getToUserId());
        if (!result.getData().toString().equals("-1")){
            //已存在
            return result;
        }
        String data = "{\"content\":\"为保障用户沟通安全，未互相关注的陌生人违规信息可能会被处理，请遵守法律法规和社会规范\"}";
        message.setData(data);
        message.setType("system");
        message.setPublicTime(new Date());

        //存储新消息并更新p2pId
        this.save(message);
        message.setP2pId(message.getId());
        this.updateById(message);

        MessageVo messageVo = new MessageVo();
        BeanUtils.copyProperties(message,messageVo);
        ContentVo contentVo = JSON.parseObject(message.getData(), ContentVo.class);
        messageVo.setData(contentVo);
        List<User> userList = this.userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, message.getFromUserId(), message.getToUserId()));
        User userA = userList.get(0);
        User userB = userList.get(1);
        UserVo toUserVo = new UserVo();
        UserVo fromUserVo = new UserVo();
        if (userA.getId() == userId) {
            BeanUtils.copyProperties(userA,fromUserVo);
            BeanUtils.copyProperties(userB,toUserVo);
        }else{
            BeanUtils.copyProperties(userB,fromUserVo);
            BeanUtils.copyProperties(userA,toUserVo);
        }
        fromUserVo.setAvatar(this.minioProperties.getUrl() + fromUserVo.getAvatar());
        toUserVo.setAvatar(this.minioProperties.getUrl() + toUserVo.getAvatar());
        messageVo.setFromUserVo(fromUserVo);
        messageVo.setToUserVo(toUserVo);

        //将信息发送给聊天对象
        return Result.success(messageVo);
    }

    @Override
    public Result gainChatMessage(Long p2pId,Long otherId) {
        UserVo fromUserVo = AuthContextHolder.getUserVo();
        Page<Message> messagePage = new Page<>(1, 10);
        QueryWrapper<Message> messageQueryWrapper = new QueryWrapper<>();
        messageQueryWrapper.eq("p2p_id",p2pId);
        messageQueryWrapper.orderByDesc("public_time");
        //添加为最后十条
        Page<Message> messagePageResult = this.messageMapper.selectPage(messagePage, messageQueryWrapper);
        List<Message> messageList = messagePageResult.getRecords();
        if (messageList.size() == 0) return Result.success(new ArrayList<>());
        messageList = messageList.stream().sorted(Comparator.comparing(Message::getPublicTime)).collect(Collectors.toList());
        UserVo toUserVo = new UserVo();
        User toUser = this.userMapper.selectById(otherId);
        BeanUtils.copyProperties(toUser,toUserVo);
        fromUserVo.setAvatar(this.minioProperties.getUrl() + fromUserVo.getAvatar());
        toUserVo.setAvatar(this.minioProperties.getUrl() + toUserVo.getAvatar());

        //转换为vo类并填入缺失的信息
        List<MessageVo> messageVoList = messageList.stream().map(message -> {
            MessageVo messageVo = new MessageVo();
            BeanUtils.copyProperties(message, messageVo);
            ContentVo contentVo = JSON.parseObject(message.getData(), ContentVo.class);
            if (contentVo.getFileType() != null){
                contentVo.setCover(this.minioProperties.getUrl() + contentVo.getCover());
                contentVo.setFileUrl(this.minioProperties.getUrl() + contentVo.getFileUrl());
            }
            messageVo.setData(contentVo);
            if (messageVo.getFromUserId() == fromUserVo.getId()) {
                messageVo.setFromUserVo(fromUserVo);
                messageVo.setToUserVo(toUserVo);
            } else {
                messageVo.setFromUserVo(toUserVo);
                messageVo.setToUserVo(fromUserVo);
            }
            return messageVo;
        }).sorted(Comparator.comparing(MessageVo::getPublicTime)).collect(Collectors.toList());

        return Result.success(messageVoList);
    }

    @Override
    public Result uploadChatFile(MultipartFile file, String suffix, Long size, Integer width,
                                 Integer height, BigDecimal duration,String type,Long otherId,Long p2pId) {

        File tempCover = null;
        String videoId = UUID.randomUUID().toString().replace("-","");

        try {
            // 判断类型，如果是视频类型使用ffmpeg去截取视频第一帧
            if ("video".equals(type)) {
                File tempVideo = File.createTempFile("video-", suffix);
                tempCover = new File(tempVideo.getParent(), videoId + ".jpg");

                // 保存文件到临时位置
                file.transferTo(tempVideo);

                System.out.println("开始截取视频封面...");
                System.out.println("视频临时文件: " + tempVideo.getAbsolutePath());
                System.out.println("封面临时文件: " + tempCover.getAbsolutePath());

                // 截取第一帧
                extractFirstFrame(tempVideo, tempCover);

                // 验证封面生成
                if (!tempCover.exists() || tempCover.length() == 0) {
                    throw new RuntimeException("封面生成失败");
                }

                // 修复：重新从临时文件读取流，而不是从MultipartFile
                try (InputStream videoStream = new FileInputStream(tempVideo)) {
                    this.minioClient.putObject(PutObjectArgs.builder()
                            .bucket(this.minioProperties.getBucketName())
                            .object(videoId + suffix)
                            .stream(videoStream, tempVideo.length(), -1)
                            .build());
                }

                // 清理临时视频文件
                tempVideo.delete();

            } else {
                // 修复：图片直接上传，只能读取一次流
                InputStream inputStream = file.getInputStream();
                this.minioClient.putObject(PutObjectArgs.builder()
                        .bucket(this.minioProperties.getBucketName())
                        .object(videoId + suffix)
                        .contentType("image/jpeg")
                        .stream(inputStream, file.getSize(), -1)
                        .build());
                inputStream.close();
            }

            String cover = null;
            // 将截取到的图片上传到minio
            if (tempCover != null && tempCover.exists() && tempCover.length() > 0) {
                cover = videoId + ".jpg";
                try (InputStream coverStream = Files.newInputStream(tempCover.toPath())) {
                    this.minioClient.putObject(PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(cover)
                            .contentType("image/jpeg")
                            .stream(coverStream, tempCover.length(), -1)
                            .build());
                }

                // 清理临时封面文件
                tempCover.delete();
            }

            // 数据库存底
            FileInfo fileInfo = new FileInfo();
            fileInfo.setUserId(AuthContextHolder.getUserId());
            fileInfo.setFileName(videoId + suffix);
            fileInfo.setCover(cover);
            fileInfo.setSize(size);
            fileInfo.setType(type);
            fileInfo.setDuration(duration);
            fileInfo.setWidth(width);
            fileInfo.setHeight(height);
            fileInfo.setSuffix(suffix);
            this.fileService.save(fileInfo);

            //消息转发
            UserVo fromUserVo = AuthContextHolder.getUserVo();
            UserVo toUserVo = new UserVo();
            User user = this.userMapper.selectById(otherId);
            BeanUtils.copyProperties(user,toUserVo);
            fromUserVo.setAvatar(this.minioProperties.getUrl() + fromUserVo.getAvatar());
            toUserVo.setAvatar(this.minioProperties.getUrl() + toUserVo.getAvatar());
            ContentVo contentVo = new ContentVo();
            contentVo.setFileType(type);
            ObjectMapper objectMapper = new ObjectMapper();
            contentVo.setCover(cover);
            contentVo.setFileUrl(fileInfo.getFileName());
            String dataJson = objectMapper.writeValueAsString(contentVo);

            contentVo.setCover(this.minioProperties.getUrl() + cover);

            contentVo.setFileUrl(this.minioProperties.getUrl() + fileInfo.getFileName());

            //存储消息po类
            Message message = new Message();
            MessageVo messageVo = new MessageVo();
            message.setType("private");
            message.setPublicTime(new Date());
            message.setFromUserId(fromUserVo.getId());
            message.setToUserId(otherId);
            message.setP2pId(p2pId);
            message.setData(dataJson);
            this.messageMapper.insert(message);

            //发送消息vo类
            BeanUtils.copyProperties(message,messageVo);
            messageVo.setFromUserVo(fromUserVo);
            messageVo.setToUserVo(toUserVo);
            messageVo.setP2pId(p2pId);
            messageVo.setData(contentVo);

            WebsocketUtil.sendToUser(otherId,messageVo);
            WebsocketUtil.sendToUser(fromUserVo.getId(),messageVo);

            return Result.success(true);

        } catch (Exception e) {
            // 清理临时文件
            if (tempCover != null && tempCover.exists()) {
                tempCover.delete();
            }
            e.printStackTrace();

        }
        return Result.success(true);
    }


    @Override
    public Result getHistoryChatMessage(Long historyChatId, Long p2pId,Long otherId) {
        UserVo selfUserVo = AuthContextHolder.getUserVo();
        QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("p2p_id",p2pId);
        queryWrapper.lt("id",historyChatId);
        queryWrapper.last("limit 10");
        queryWrapper.orderByDesc("public_time");
        List<Message> chatMessageList = this.messageMapper.selectList(queryWrapper);
        if (chatMessageList.size() == 0) return Result.success(new ArrayList<>());

        User otherUser = this.userMapper.selectById(otherId);
        UserVo otherUserVo = new UserVo();
        BeanUtils.copyProperties(otherUser,otherUserVo);
        otherUserVo.setAvatar(this.minioProperties.getUrl() + otherUserVo.getAvatar());
        List<MessageVo> messageVoList = chatMessageList.stream().map(message -> {
            MessageVo messageVo = new MessageVo();
            ContentVo contentVo = new ContentVo();
            BeanUtils.copyProperties(message, messageVo);
            contentVo = JSON.parseObject(message.getData(), ContentVo.class);
            if (contentVo.getFileType() != null) {
                contentVo.setFileUrl(this.minioProperties.getUrl() + contentVo.getFileUrl());
                contentVo.setCover(this.minioProperties.getUrl() + contentVo.getCover());
            }
            messageVo.setData(contentVo);
            if (messageVo.getFromUserId() == selfUserVo.getId()) {
                messageVo.setFromUserVo(selfUserVo);
                messageVo.setToUserVo(otherUserVo);
            } else {
                messageVo.setFromUserVo(otherUserVo);
                messageVo.setToUserVo(selfUserVo);
            }
            return messageVo;
        }).sorted(Comparator.comparing(MessageVo::getId)).collect(Collectors.toList());

        return Result.success(messageVoList);
    }


    private void extractFirstFrame(File video, File cover) throws Exception {
        // 核心修复：添加 -update 1 参数
        String command = String.format("%s -loglevel error -i \"%s\" -ss 00:00:00.5 -vframes 1 -q:v 2 -update 1 \"%s\" -y",
                ffmpegPath, video.getAbsolutePath(), cover.getAbsolutePath());

        System.out.println("FFmpeg命令: " + command);

        Process process = Runtime.getRuntime().exec(command);

        // 处理输出流
        handleProcessStreams(process);

        // 等待进程完成
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("FFmpeg处理超时");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg执行失败，退出码: " + exitCode);
        }

        // 等待文件写入
        Thread.sleep(200);

        if (!cover.exists()) {
            throw new RuntimeException("封面文件未生成: " + cover.getAbsolutePath());
        }
    }
    private void handleProcessStreams(Process process) {
        // 处理标准输出
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("FFmpeg输出: " + line);
                }
            } catch (IOException e) {
                // 忽略
            }
        }).start();

        // 处理错误输出
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.err.println("FFmpeg错误: " + line);
                }
            } catch (IOException e) {
                // 忽略
            }
        }).start();
    }
}
