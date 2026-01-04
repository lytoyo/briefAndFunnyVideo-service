package com.lytoyo.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Package:com.lytoyo.common.properties
 *
 * @ClassName:MinioProperties
 * @Create:2025/12/15 11:10
 **/
@ConfigurationProperties(prefix = "minio")
@Component
@Data
public class MinioProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private Boolean secure;
    private String url;
}
