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
import com.lytoyo.mapper.UserMapper;
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
import java.util.*;
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

    private static UserMapper userMapper;

    private static MessageService messageService;

    private static MessageMapper messageMapper;

    private static AttentionMapper attentionMapper;

    @Autowired
    public void setUserService(UserServiceImpl userService) {
        SystemWebsocket.userService = userService;
    }

    @Autowired
    public void setUserMapper(UserMapper userMapper) {
        SystemWebsocket.userMapper = userMapper;
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
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(user, userVo);
        //用户信息绑定添加
        WebsocketUtil.addUser(userId, userVo);
        WebsocketUtil.addSession(userId, session);
        WebsocketUtil.getDownlineTimeMessage(user);

    }

    @OnMessage
    public void onMessage(String msg, Session session) {
        System.out.println(msg);

        MessageVo messageVo = JSON.parseObject(msg,MessageVo.class);
        //忽略心跳包
        if (messageVo.getType().equals("heartbeat")){
            return;
        }
        WebsocketUtil.sendMessage(messageVo);

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
