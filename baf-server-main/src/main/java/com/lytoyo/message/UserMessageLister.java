package com.lytoyo.message;

import com.lytoyo.common.domain.User;
import com.lytoyo.common.domain.vo.UserVo;
import com.lytoyo.repository.UserVoRepository;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.channels.Channel;

/**
 * Package:com.lytoyo.message
 *
 * @ClassName:UserMessageLister
 * @Create:2026/1/5 9:15
 **/
@Component
public class UserMessageLister {

    @Resource
    private UserVoRepository userVoRepository;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(durable = "true",value = "queue.baf.up.elasticsearch.user"),
    exchange = @Exchange(value = "exchange.direct.baf"),key = {"routing.baf.up.user"}))
    public void userUpElasticsearchProcessMessage(User user, Message message, Channel channel){
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(user,userVo);
        userVoRepository.save(userVo);
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(durable = "true",value = "queue.baf.mod.user"),
    exchange = @Exchange(value = "exchange.direct.baf"),key = {"routing.baf.mod.user"}))
    public void userModElasticsearchProcessMessage(User user,Message message,Channel channel){

    }
}
