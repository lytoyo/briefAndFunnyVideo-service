package com.lytoyo.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Package:com.lytoyo.framework.config
 *
 * @ClassName:EmailProperties
 * @Create:2025/12/2 11:15
 **/
@ConfigurationProperties(prefix = "email.config")
@Configuration
//@EnableConfigurationProperties
@Data
public class EmailProperties {
    /**
     * 邮箱地址
     */
    private String user;
    /**
     * 发件人昵称（必须正确，否则发送失败）
     */
    private String from;
    /**
     * 邮件服务器的SMTP地址
     */
    private String host;

    /**
     * 邮件服务器的SMTP端口
     */
    private Integer port;

    /**
     * 密码（授权码）
     */
    private String password;

    /**
     * 验证码过期时间
     */
    private Integer expireTime;



}


