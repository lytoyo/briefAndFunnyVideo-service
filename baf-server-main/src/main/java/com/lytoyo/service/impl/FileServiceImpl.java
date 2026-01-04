package com.lytoyo.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lytoyo.common.constant.RabbitMqConstant;
import com.lytoyo.common.domain.FileInfo;
import com.lytoyo.common.properties.MinioProperties;
import com.lytoyo.common.utils.AuthContextHolder;
import com.lytoyo.common.utils.RabbitMqUtil;
import com.lytoyo.mapper.FileMapper;
import com.lytoyo.service.FileService;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                             BigDecimal duration, Integer width, Integer height) {
        List<ComposeSource> sources = new ArrayList<>();
        boolean flag = false;
        try{
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
            //分片合并
            minioClient.composeObject(ComposeObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(fileName)
                    .sources(sources)
                    .build());
            Long userId = AuthContextHolder.getUserId();
            FileInfo fileInfo = new FileInfo();
            String url = minioProperties.getUrl()+fileName;
            fileInfo.setFileName(fileName)
                    .setUserId(userId)
                    .setSuffix(suffix)
                    .setSize(size)
                    .setType(type)
                    .setDuration(duration)
                    .setWidth(width)
                    .setHeight(height)
                    .setUrl(url);
            //保存文件
            flag = this.save(fileInfo);
            // 异步删除残留文件分片
            HashMap<String, Object> data = new HashMap<>();
            data.put("md5",md5);
            data.put("chunkCount",chunkCount);
            rabbitMqUtil.sendFileZoneMessage(RabbitMqConstant.DIRECTEXCHANGE,RabbitMqConstant.FILE_ROUTINGKEY,data);
        }catch (Exception e){
            e.printStackTrace();
        }
        return flag;
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
        //视频文件实际大小
        long contentRangeSize = 0;
        //视频文b件分段字节数组的开头下标
        long contentRangeStart = 0;
        //视频文件分段字节数组的结尾下标
        long contentRangeEnd = 0;
        StatObjectResponse stat = null;

        try {
            outputStream = response.getOutputStream();
            // 1. 先获取文件总大小
            stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(videoName)
                    .build());
            contentRangeSize = stat.size();

            // 2. 解析Range头，有俩种格式，bytes=0- 和 bytes=0-1024
            String rangeString = request.getHeader(HttpHeaders.RANGE);
            if (StrUtil.isNotBlank(rangeString)) {
                String[] rangeArr = rangeString.split("=");
                //bytes=0-1024情况
                if (rangeArr.length > 1) {
                    String[] rangeData = rangeArr[1].split("-");
                    contentRangeStart = Long.parseLong(rangeData[0]);
                    // 关键：当没有结束位置时，自动设置为文件末尾
                    contentRangeEnd = (rangeData.length > 1 && !rangeData[1].isEmpty()) ?
                            Long.parseLong(rangeData[1]) :
                            contentRangeSize - 1;
                }
                // 确保结束位置不超过文件范围
                contentRangeEnd = Math.min(contentRangeEnd, contentRangeSize - 1);
            }

            // 3. 统一处理所有Range请求（包括无结束位置的情况）
            long chunkLength = contentRangeEnd - contentRangeStart + 1;

            // 设置响应头
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader(HttpHeaders.CONTENT_TYPE, "video/mp4");
            response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
            response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(chunkLength));
            response.setHeader(HttpHeaders.CONTENT_RANGE,
                    String.format("bytes %d-%d/%d", contentRangeStart, contentRangeEnd, contentRangeSize));

            // 4. 流式传输分片数据
            try (GetObjectResponse objectResponse = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(videoName)
                            .offset(contentRangeStart)
                            .length(chunkLength)
                            .build())) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = objectResponse.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } finally {
                try {
                    if (null != outputStream) {
                        outputStream.close();
                    }
                } catch (Exception e) {
                    log.error("流操作已失效");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            try {
                outputStream.flush();
                if (null != outputStream) {
                    outputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
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
            minioClient.putObject(PutObjectArgs.builder()
                       .bucket(minioProperties.getBucketName())
                       .object(fileName)
                       .stream(file.getInputStream(), file.getSize(), -1)
                       .build());
            FileInfo fileInfo = new FileInfo();
            String url = minioProperties.getUrl()+fileName;
            Long userId = AuthContextHolder.getUserId();
            fileInfo.setFileName(fileName)
                    .setUserId(userId)
                    .setSuffix(suffix)
                    .setSize(size)
                    .setType(type)
                    .setDuration(duration)
                    .setWidth(width)
                    .setHeight(height)
                    .setUrl(url);
            //保存文件信息
            flag = this.save(fileInfo);
        }catch (Exception e){
            e.printStackTrace();
        }
        return flag;
    }

}
