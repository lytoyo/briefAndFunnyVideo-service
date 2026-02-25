package com.lytoyo.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * Package:com.lytoyo.framework.config
 *
 * @ClassName:WebsocketConfig
 * @Create:2026/1/5 13:43
 **/
@Configuration
public class WebsocketConfig {
    @Bean
    public ServerEndpointExporter serverEndpointExporter(){
        return new ServerEndpointExporter();
    }
}
