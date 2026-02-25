package com.lytoyo.message;

import com.lytoyo.common.constant.RabbitMqConstant;
import com.lytoyo.common.properties.MinioProperties;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.errors.*;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import java.io.IOException;
import java.nio.channels.Channel;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;



/**
 * Package:com.lytoyo.message
 *
 * @ClassName:FileMessageLister
 * @Create:2025/12/29 10:28
 **/
@Component
public class FileMessageLister {

    @Resource
    private MinioClient minioClient;

    @Resource
    private MinioProperties minioProperties;


    /**
     * 删除文件分片
     * @param dataString
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(durable = "true", value = "queue.baf.file"),
            exchange = @Exchange(value = "exchange.direct.baf"), key = {"routing.baf.file"}))
    public void fileProcessMessage(Map<String, Object> dataString) {
        String md5 = (String) dataString.get("md5");
        Integer chunkCount = (Integer) dataString.get("chunkCount");
        try {
            for (int i = 0; i < chunkCount; i++) {
                String templateName = md5 + "_" + i;
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .object(templateName).bucket(minioProperties.getBucketName())
                        .build());
            }
        } catch (Exception e) {
        }
    }
}
