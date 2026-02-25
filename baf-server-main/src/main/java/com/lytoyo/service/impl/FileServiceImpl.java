package com.lytoyo.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lytoyo.common.constant.RabbitMqConstant;
import com.lytoyo.common.domain.FileInfo;
import com.lytoyo.common.domain.Result;
import com.lytoyo.common.properties.MinioProperties;
import com.lytoyo.common.utils.AuthContextHolder;
import com.lytoyo.common.utils.RabbitMqUtil;
import com.lytoyo.mapper.FileMapper;
import com.lytoyo.service.FileService;
import io.minio.*;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import io.minio.Directive;

/**
 * Package:com.lytoyo.service.impl
 *
 * @ClassName:FileServiceImpl
 * @Create:2025/12/15 10:54
 **/
@Service
@Slf4j
public class FileServiceImpl extends ServiceImpl<FileMapper, FileInfo> implements FileService {

    @Resource
    private MinioClient minioClient;

    @Resource
    private MinioProperties minioProperties;

    @Resource
    private RabbitMqUtil rabbitMqUtil;

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    /**
     * 文件分片上传
     */
    @Override
    public void uploadZone(MultipartFile file,String md5,Integer chunkIndex) {
        try{
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(md5 +"_"+chunkIndex)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .build());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 文件分片合并
     * @return
     */
    @Override
    public boolean zoneMerge(String md5, String suffix, Long size, String type, Integer chunkCount,
                             BigDecimal duration, Integer width, Integer height) throws Exception {
        List<ComposeSource> sources = new ArrayList<>();
        boolean flag = false;
            for (int i = 0; i < chunkCount; i++){
                String templateName =  md5 + "_" + i;
                //判断是否所有分片都已经上传
                flag = isFileExist(templateName);
                for (int j = 0; j < 3; j++){
                    if (!flag){
                        //当前分片未上传，轮询3次，每次等待3秒
                        Thread.sleep(3000);
                        flag = isFileExist(templateName);
                    }else{
                        break;
                    }
                }
                //如果轮询三次都没有拿到分片，可能分片上传途中发生错误，需要通知前端重新上传
                if (!flag) return flag;
                sources.add(ComposeSource.builder().bucket(minioProperties.getBucketName()).object(templateName).build());
            }
            String fileName = md5 + suffix;
            //合并分片并设置contentType
            mergeChunksWithContentType(sources,fileName,type);
            Long userId = AuthContextHolder.getUserId();
            FileInfo fileInfo = new FileInfo();

            //todo 暂时演示在windows系统上截取视频文件第一帧，上线linux还需修改
        // TODO: 2026/1/9 例如截取的图片无法直接上传到minio，要想办法解决
            if (type.equals("video")){
                String videoName = "d658208e5ec5ba9e15bde72435f92509.mp4";
                String imagePath = captureFirstFrame(videoName);
                fileInfo.setCover(imagePath);
            }

            fileInfo.setFileName(fileName)
                    .setUserId(userId)
                    .setSuffix(suffix)
                    .setSize(size)
                    .setType(type)
                    .setDuration(duration)
                    .setWidth(width)
                    .setHeight(height);
            //保存文件
            flag = this.save(fileInfo);

            // 异步删除残留文件分片
            HashMap<String, Object> data = new HashMap<>();
            data.put("md5",md5);
            data.put("chunkCount",chunkCount);
            rabbitMqUtil.sendFileZoneMessage(RabbitMqConstant.DIRECTEXCHANGE,RabbitMqConstant.FILE_ROUTINGKEY,data);

        return flag;
    }

    /**
     * 合并分片并设置contentType
     * @param sources
     * @param fileName
     * @param type
     */
    private void mergeChunksWithContentType(List<ComposeSource> sources, String fileName,String type) throws Exception {
        //分片合并
            if (type.equals("image")){
                String tempFileName = "temp_"+fileName;
                //合并为临时文件
                minioClient.composeObject(ComposeObjectArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .object(tempFileName)
                                .sources(sources)
                                .build());
                CopySource source = CopySource.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(tempFileName)
                        .build();
                //设置contentType并复制临时文件
                minioClient.copyObject(
                        CopyObjectArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .object(fileName)
                                .source(source)
                                .headers(new HashMap<String, String>() {{
                                    put("Content-Type", "image/jpeg");
                                }})
                                .metadataDirective(Directive.REPLACE)
                                .build()
                );
                //删除临时文件
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .object(tempFileName)
                                .build()
                );
            }else{
                minioClient.composeObject(ComposeObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(fileName)
                        .sources(sources)
                        .build());
            }

    }


    /**
     * 截取视频文件第一帧
     * @param videoName
     * @return
     */
    public static String captureFirstFrame(String videoName) {
        String videoPath = "D:\\毕设项目\\testFile\\" + videoName;
        String imagePath = "D:\\毕设项目\\testFile\\"+ UUID.randomUUID().toString().replace("-","") + ".jpg";
        FFmpegFrameGrabber grabber = null;

        try {
             grabber = new FFmpegFrameGrabber(videoPath);
            // 2. 设置解码格式（提高解码速度）
            grabber.setOption("rtsp_transport", "tcp");
            grabber.setFormat("mp4");

            // 3. 开始抓取
            grabber.start();

            // 4. 获取第一帧
            Frame frame = grabber.grabImage();

            if (frame == null) {
                throw new IOException("无法获取视频帧，可能是空视频或格式不支持");
            }

            // 5. 转换为BufferedImage
            Java2DFrameConverter converter = new Java2DFrameConverter();
            BufferedImage bufferedImage = converter.getBufferedImage(frame);

            // 6. 保存为图片
            String format = "jpg";
            ImageIO.write(bufferedImage, format, new File(imagePath));

            return imagePath;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }finally {
            // 7. 关闭资源
            if (grabber != null) {
                try {
                    grabber.stop();
                    grabber.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * 判断文件分片是否存在
     * @param templateName
     * @return
     */
    private boolean isFileExist(String templateName) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(templateName)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 视频分片请求
     * @param videoName
     * @param request
     * @param response
     */
    @Override
    public void zoneRequest(String videoName, HttpServletRequest request, HttpServletResponse response) {
        ServletOutputStream outputStream = null;
        response.reset();

        try {
            // 1. 先获取文件总大小
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(videoName)
                    .build());
            long fileSize = stat.size();

            // 2. 设置默认值
            long start = 0;
            long end = fileSize - 1;
            long chunkLength = fileSize;
            boolean isRangeRequest = false;

            // 3. 解析Range头部
            String rangeHeader = request.getHeader(HttpHeaders.RANGE);
            if (StrUtil.isNotBlank(rangeHeader) && rangeHeader.startsWith("bytes=")) {
                isRangeRequest = true;
                try {
                    // 格式: bytes=0- 或 bytes=0-1024
                    String rangeValue = rangeHeader.substring(6);
                    String[] rangeParts = rangeValue.split("-");

                    if (rangeParts.length > 0 && !rangeParts[0].isEmpty()) {
                        start = Long.parseLong(rangeParts[0]);
                    }

                    if (rangeParts.length > 1 && !rangeParts[1].isEmpty()) {
                        end = Long.parseLong(rangeParts[1]);
                    } else {
                        // 如果没有结束位置，设置为文件末尾
                        end = fileSize - 1;
                    }

                    // 验证范围
                    if (start < 0) start = 0;
                    if (end >= fileSize) end = fileSize - 1;
                    if (start > end) {
                        // 如果start大于end，交换它们
                        long temp = start;
                        start = end;
                        end = temp;
                    }

                    chunkLength = end - start + 1;

                    // 关键：确保chunkLength大于0
                    if (chunkLength <= 0) {
                        throw new IllegalArgumentException("Invalid range: chunkLength <= 0");
                    }


                } catch (NumberFormatException e) {
                    log.warn("Range头部格式错误: {}", rangeHeader, e);
                    // 如果Range头部格式错误，返回整个文件
                    start = 0;
                    end = fileSize - 1;
                    chunkLength = fileSize;
                }
            } else {
                log.info("无Range头部，返回整个文件: fileSize={}", fileSize);
            }

            // 4. 设置响应头
            if (isRangeRequest) {
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                response.setHeader(HttpHeaders.CONTENT_RANGE,
                        String.format("bytes %d-%d/%d", start, end, fileSize));
            } else {
                response.setStatus(HttpServletResponse.SC_OK);
            }

            response.setHeader(HttpHeaders.CONTENT_TYPE, "video/mp4");
            response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
            response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(chunkLength));

            // 添加跨域支持
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Expose-Headers", "Content-Range, Accept-Ranges, Content-Length");

            // 5. 获取并传输数据
            outputStream = response.getOutputStream();

            try (GetObjectResponse objectResponse = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(videoName)
                            .offset(start)
                            .length(chunkLength)
                            .build())) {

                byte[] buffer = new byte[8192]; // 使用8KB缓冲区
                int bytesRead;
                long totalBytesWritten = 0;

                while (totalBytesWritten < chunkLength &&
                        (bytesRead = objectResponse.read(buffer)) != -1) {
                    // 确保不会写入超过chunkLength的数据
                    long remaining = chunkLength - totalBytesWritten;
                    int bytesToWrite = (int) Math.min(bytesRead, remaining);

                    if (bytesToWrite > 0) {
                        try {
                            outputStream.write(buffer, 0, bytesToWrite);
                            totalBytesWritten += bytesToWrite;
                            outputStream.flush();
                        } catch (ClientAbortException e) {
                            log.debug("客户端中断连接，停止传输");
                            break;
                        }
                    }

                    if (totalBytesWritten >= chunkLength) {
                        break;
                    }
                }


            } catch (ClientAbortException e) {
                log.debug("客户端在传输过程中中断连接");
            } catch (Exception e) {
                log.error("数据传输异常", e);
                if (!response.isCommitted()) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }

        } catch (Exception e) {
            log.error("处理视频请求异常", e);
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try {
                    response.getWriter().write("视频处理失败: " + e.getMessage());
                } catch (IOException ignored) {}
            }
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    log.debug("输出流关闭异常", e);
                }
            }
        }
    }

    /**
     * 上传简单文件
     * @return
     */
    @Override
    public boolean smallFileUpload(MultipartFile file, String md5, String suffix, Long size,
                                   String type, Integer width, Integer height, BigDecimal duration) {
        boolean flag = false;
        try{
            String fileName = md5 + suffix;
            String contentType = type.equals("image")?"image/jpeg":"application/octet-stream";
            minioClient.putObject(PutObjectArgs.builder()
                       .bucket(minioProperties.getBucketName())
                       .object(fileName)
                       .contentType(contentType)
                       .stream(file.getInputStream(), file.getSize(), -1)
                       .build());
            FileInfo fileInfo = new FileInfo();

            Long userId = AuthContextHolder.getUserId();
            if (type.equals("video")){
                String videoName = "d658208e5ec5ba9e15bde72435f92509.mp4";
                String imagePath = captureFirstFrame(videoName);
                fileInfo.setCover(imagePath);
            }
            fileInfo.setFileName(fileName)
                    .setUserId(userId)
                    .setSuffix(suffix)
                    .setSize(size)
                    .setType(type)
                    .setDuration(duration)
                    .setWidth(width)
                    .setHeight(height);
            //保存文件信息
            flag = this.save(fileInfo);
        }catch (Exception e){
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * 聊天文件上传
     * @param file
     * @param suffix
     * @param size
     * @param type
     * @param width
     * @param height
     * @param duration
     * @return
     */
    @Override
    public Result commentFileUpload(MultipartFile file, String suffix, Long size, String type, Integer width,
                                    Integer height, BigDecimal duration) throws Exception {
        Map<String, Object> result = new HashMap<>();
        File tempCover = null;
        String videoId = UUID.randomUUID().toString();

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
            this.save(fileInfo);

            // 回显封面图片名称+文件名称+封面图片地址+文件地址
            result.put("cover", cover);
            result.put("fileName", videoId + suffix);
            result.put("coverUrl", this.minioProperties.getUrl() + (cover != null ? cover : ""));
            result.put("fileUrl", this.minioProperties.getUrl() + videoId + suffix);
            result.put("id",fileInfo.getId());
            return Result.success(result);

        } catch (Exception e) {
            // 清理临时文件
            if (tempCover != null && tempCover.exists()) {
                tempCover.delete();
            }
            e.printStackTrace();
            throw e;
        }
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
