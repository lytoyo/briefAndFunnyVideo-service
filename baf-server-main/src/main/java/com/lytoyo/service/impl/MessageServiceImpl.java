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
    public Result gainChatMessage(Long p2pId,Long otherId,Integer current,Integer size) {
        UserVo fromUserVo = AuthContextHolder.getUserVo();

        QueryWrapper<Message> messageQueryWrapper = new QueryWrapper<>();
        messageQueryWrapper.eq("p2p_id", p2pId);
        // 先按时间倒序，确保最晚发出的消息在最前面被计数
        messageQueryWrapper.orderByDesc("public_time");

        // 【修改点2】放弃 Page 对象，改用精准的 LIMIT OFFSET 物理分页
        // 含义：跳过前 current 条数据，获取接下来的 size 条数据
        messageQueryWrapper.last("limit " + size + " offset " + current);

        List<Message> messageList = this.messageMapper.selectList(messageQueryWrapper);

        if (messageList == null || messageList.size() == 0) {
            return Result.success(new ArrayList<>());
        }

        // 获取对方的用户信息
        UserVo toUserVo = new UserVo();
        User toUser = this.userMapper.selectById(otherId);
        BeanUtils.copyProperties(toUser, toUserVo);
        fromUserVo.setAvatar(this.minioProperties.getUrl() + fromUserVo.getAvatar());
        toUserVo.setAvatar(this.minioProperties.getUrl() + toUserVo.getAvatar());

        // 转换为vo类并填入缺失的信息
        List<MessageVo> messageVoList = messageList.stream().map(message -> {
                    MessageVo messageVo = new MessageVo();
                    BeanUtils.copyProperties(message, messageVo);
                    ContentVo contentVo = JSON.parseObject(message.getData(), ContentVo.class);
                    if (contentVo.getFileType() != null){
                        contentVo.setCover(this.minioProperties.getUrl() + contentVo.getCover());
                        contentVo.setFileUrl(this.minioProperties.getUrl() + contentVo.getFileUrl());
                    }
                    messageVo.setData(contentVo);
                    if (messageVo.getFromUserId().equals(fromUserVo.getId())) {
                        messageVo.setFromUserVo(fromUserVo);
                        messageVo.setToUserVo(toUserVo);
                    } else {
                        messageVo.setFromUserVo(toUserVo);
                        messageVo.setToUserVo(fromUserVo);
                    }
                    return messageVo;
                })
                // 【保持原代码的正序排序】确保返回给前端的列表，时间越早的越排在前面
                .sorted(Comparator.comparing(MessageVo::getPublicTime))
                .collect(Collectors.toList());

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
    public Result getHistoryChatMessage(Long p2pId, Long otherId, Integer current, Integer size) {
        UserVo selfUserVo = AuthContextHolder.getUserVo();
        QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("p2p_id", p2pId);

        // 【修改点1】 使用 id 倒序，这样最新的消息 id 最大。
        //            从最新的消息往前取，OFFSET current，LIMIT size。
        //            这样获取到的消息列表是按 ID 降序排列的 (即最新的消息在前面)。
        queryWrapper.orderByDesc("id");

        // 【修改点2】 实现物理分页：跳过前 current 条数据，获取接下来的 size 条数据
        // 注意：这里的 current 对应 SQL 的 OFFSET，size 对应 SQL 的 LIMIT
        // 第一次请求 current=0, size=10 -> limit 10 offset 0
        // 第二次请求 current=10, size=10 -> limit 10 offset 10
        queryWrapper.last("limit " + size + " offset " + current);

        List<Message> chatMessageList = this.messageMapper.selectList(queryWrapper);

        if (chatMessageList == null || chatMessageList.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 获取当前用户 (selfUserVo) 和对方用户信息 (otherUserVo)
        User otherUser = this.userMapper.selectById(otherId);
        UserVo otherUserVo = new UserVo();
        // 检查 otherUser 是否为空，避免空指针
        if (otherUser != null) {
            BeanUtils.copyProperties(otherUser, otherUserVo);
            otherUserVo.setAvatar(this.minioProperties.getUrl() + otherUserVo.getAvatar());
        }

        // 转换为 MessageVo 并填充用户信息
        List<MessageVo> messageVoList = chatMessageList.stream().map(message -> {
                    MessageVo messageVo = new MessageVo();
                    ContentVo contentVo = new ContentVo();
                    BeanUtils.copyProperties(message, messageVo);
                    contentVo = JSON.parseObject(message.getData(), ContentVo.class);

                    if (contentVo != null && contentVo.getFileType() != null) {
                        contentVo.setFileUrl(this.minioProperties.getUrl() + contentVo.getFileUrl());
                        contentVo.setCover(this.minioProperties.getUrl() + contentVo.getCover());
                    }
                    messageVo.setData(contentVo);

                    // 根据消息的发送者填充 FromUserVo 和 ToUserVo
                    // 这里需要确保 fromUserVo 和 toUserVo 在方法外部或循环内能正确获取到
                    // 从当前登录用户和 otherUser 中判断消息的发送方和接收方
                    if (messageVo.getFromUserId().equals(selfUserVo.getId())) {
                        messageVo.setFromUserVo(selfUserVo);
                        messageVo.setToUserVo(otherUserVo);
                    } else {
                        messageVo.setFromUserVo(otherUserVo);
                        messageVo.setToUserVo(selfUserVo);
                    }
                    return messageVo;
                })
                // 【修改点3】对消息列表进行排序。
                //    由于数据库是按 ID 倒序查询的，所以默认 messageVoList 是从新到旧的顺序。
                //    为了保持前端 unshift 时，历史消息按时间从旧到新排列，这里可以：
                //    1. 不排序，让前端收到从新到旧的列表后，自己 `reverse()` 再 `unshift()`。
                //    2. 在后端进行 `reverse()`，让返回的列表就是从旧到新。
                //    这里我们选择在后端进行 `reverse()` 操作，保持返回数据逻辑一致，以便前端直接 unshift。
                .sorted(Comparator.comparing(MessageVo::getId)) // 再次按ID升序排序，使最旧的消息在前面
                .collect(Collectors.toList());

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
