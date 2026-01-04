package com.lytoyo.framework.config;

import com.lytoyo.common.properties.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * Package:com.lytoyo.framework.config
 *
 * @ClassName:MinioConfig
 * @Create:2025/12/8 10:50
 **/
@Slf4j
@Data
@Configuration
public class MinioConfig {

    @Resource
    private MinioProperties minioProperties;

    @Bean
    public MinioClient minioClient() {
        MinioClient minioClient = null;
        try{
            minioClient = MinioClient.builder()
                    .endpoint(minioProperties.getEndpoint())
                    .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                    .build();
            boolean bucketExist = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.getBucketName()).build());
            if(!bucketExist){
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucketName()).build());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        log.info("minio初始化完成");
        return minioClient;
    }
}
