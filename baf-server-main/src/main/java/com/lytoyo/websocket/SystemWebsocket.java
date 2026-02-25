package com.lytoyo.websocket;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lytoyo.common.constant.RedisConstant;
import com.lytoyo.common.domain.Attention;
import com.lytoyo.common.domain.Message;
import com.lytoyo.common.domain.User;
import com.lytoyo.common.domain.vo.MessageVo;
import com.lytoyo.common.domain.vo.UserVo;
import com.lytoyo.common.utils.WebsocketUtil;
import com.lytoyo.mapper.AttentionMapper;
import com.lytoyo.mapper.MessageMapper;
import com.lytoyo.service.MessageService;
import com.lytoyo.service.UserService;
import com.lytoyo.service.impl.UserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Package:com.lytoyo.websocket
 *
 * @ClassName:SystemWebsocket
 * @Create:2026/1/5 13:32
 **/
@Component
@ServerEndpoint(value = "/system/{userId}")
@Slf4j
public class SystemWebsocket {

    private static UserService userService;

    private static MessageService messageService;

    private static MessageMapper messageMapper;

    private static AttentionMapper attentionMapper;

    @Autowired
    public void setUserService(UserServiceImpl userService) {
        SystemWebsocket.userService = userService;
    }

    @Autowired
    public void setMessageService(MessageService messageService) {
        SystemWebsocket.messageService = messageService;
    }

    @Autowired
    public void setMessageMapper(MessageMapper messageMapper) {
        SystemWebsocket.messageMapper = messageMapper;
    }

    @Autowired
    private void setAttentionMapper(AttentionMapper attentionMapper) {
        SystemWebsocket.attentionMapper = attentionMapper;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        User user = userService.getById(userId);

        //查看当前用户关注对象
        List<Long> postHostUserIdList = attentionMapper.selectList(new LambdaQueryWrapper<Attention>()
                        .eq(Attention::getFansUserId, userId))
                        .stream()
                        .map(attention -> {
                            //贴主id
                            Long postHostUserId = attention.getUserId();
                            return postHostUserId;
                        }).collect(Collectors.toList());

        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(user, userVo);
        //用户信息绑定添加
        WebsocketUtil.addUser(userId, userVo);
        WebsocketUtil.addSession(userId, session);

        long downlineTime = user.getDownlineTime().getTime();
        //每次重新连接websocket都要比较消息队列表里发布时间与用户最近离线时间
        //拉取前面未登录而错过的消息并剔除关注推送消息
        List<MessageVo> messageVoList = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                        .in(Message::getToUserId,userId,0,-1)
                        .ge(Message::getTimestamp, downlineTime))
                .stream().filter(f -> {
                    boolean flag = true;
                    //条件：这条为关注推送消息，用户的关注列表大于0.这条关注推送消息发送者是用户关注列表里的
                    if (f.getMessageTypeCode() == 2){
                        if (postHostUserIdList.size() > 0 && postHostUserIdList.contains(f.getFromUserId())){
                            flag = true;
                        }else{
                            flag = false;
                        }
                    }
                    return flag;
                })
                .map(message -> {
                    MessageVo messageVo = new MessageVo();
                    BeanUtils.copyProperties(message, messageVo);
                    messageVo.setData(JSON.parse(message.getData()));
                    return messageVo;
                }).collect(Collectors.toList());
        //发送未读取的消息列表
        WebsocketUtil.sendMessageToObject(messageVoList);
    }

    @OnMessage
    public void onMessage(String msg, Session session) {
        //构建发送给用户交换的消息体
        MessageVo messageVo = JSONObject.parseObject(msg, MessageVo.class);
        List<MessageVo> messageVoList = Arrays.asList(messageVo);
        //存储消息体
        WebsocketUtil.storeMessage(messageVo);
        //发送消息
        WebsocketUtil.sendMessageToObject(messageVoList);

    }

    @OnClose
    public void onClose(Session session, @PathParam("userId") Long userId) {
        WebsocketUtil.removeUser(userId);
        WebsocketUtil.removeSession(userId);
        //只要断开websocket连接就算下线，记录最近下线时间
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", userId);
        updateWrapper.set("downline_time", DateUtil.date());
        userService.update(updateWrapper);
    }

    @OnError
    public void onError(Session session, Throwable e) {
        log.info("websocket出现异常:", e);
    }

}
