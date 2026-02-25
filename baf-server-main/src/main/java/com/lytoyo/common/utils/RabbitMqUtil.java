package com.lytoyo.common.utils;

import com.lytoyo.common.domain.Blog;
import com.lytoyo.common.domain.User;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

/**
 * Package:com.lytoyo.common.utils
 *
 * @ClassName:RabbitMqUtil
 * @Create:2025/12/29 9:58
 **/
@Component
public class RabbitMqUtil {
    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 文件分片删除
     * @param exchange
     * @param routingKey
     * @param data
     */
    public void sendFileZoneMessage(String exchange, String routingKey, Map data){
        rabbitTemplate.convertAndSend(exchange,routingKey,data);
    }

    public void sendBlogToElasticsearch(String exchange, String routingKey, Blog blog){
        rabbitTemplate.convertAndSend(exchange,routingKey,blog);
    }


    public void sendUserToElasticsearch(String exchange, String routingKey, User user){
        rabbitTemplate.convertAndSend(exchange,routingKey,user);
    }


}
