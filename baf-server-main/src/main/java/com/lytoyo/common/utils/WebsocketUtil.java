package com.lytoyo.common.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lytoyo.common.domain.Attention;
import com.lytoyo.common.domain.Message;
import com.lytoyo.common.domain.User;
import com.lytoyo.common.domain.vo.ContentVo;
import com.lytoyo.common.domain.vo.MessageVo;
import com.lytoyo.common.domain.vo.UserVo;
import com.lytoyo.common.properties.MinioProperties;
import com.lytoyo.mapper.AttentionMapper;
import com.lytoyo.mapper.MessageMapper;
import com.lytoyo.mapper.UserMapper;
import com.lytoyo.service.AttentionService;
import com.lytoyo.service.MessageService;
import com.lytoyo.websocket.SystemWebsocket;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.websocket.Session;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Package:com.lytoyo.common.utils
 *
 * @ClassName:WebsocketUtil
 * @Create:2026/1/5 13:45
 **/
@Slf4j
@Component
public class WebsocketUtil {

    //建立userId与userInfo基本信息对应的容器，通过userId可以获取用户基本信息
    private static Map<Long, UserVo> userMap = new ConcurrentHashMap<>();

    //建立userId和ws Session的对应容器，通过userId获取Session会话信息
    private static Map<Long, Session> sessionMap = new ConcurrentHashMap<>();

    public static Set<Long> getAllUserId() {
        return userMap.keySet();
    }

    private static MessageService messageService;

    private static AttentionService attentionService;

    private static AttentionMapper attentionMapper;

    private static MessageMapper messageMapper;

    private static UserMapper userMapper;

    private static MinioProperties minioProperties;

    @Autowired
    public  void setMinioProperties(MinioProperties minioProperties){
        WebsocketUtil.minioProperties = minioProperties;
    }

    @Autowired
    public void setMessageMapper(MessageMapper messageMapper){
        WebsocketUtil.messageMapper = messageMapper;
    }

    @Autowired
    public  void setUserMapper(UserMapper userMapper){
        WebsocketUtil.userMapper = userMapper;
    }

    public static void sendMessage(MessageVo messageVo) {
        
        messageVo.setPublicTime(new Date());
        Message message = new Message();
        BeanUtils.copyProperties(messageVo,message);
        if (messageVo.getData().getFileType() != null) {
            ContentVo contentVo = messageVo.getData();
            String[] tempUrls = contentVo.getFileUrl().split("/");
            System.out.println(tempUrls[tempUrls.length-1]);
            contentVo.setFileUrl(tempUrls[tempUrls.length-1]);
            if (contentVo.getFileType().equals("video")){
                String[] coverUrls = contentVo.getCover().split("/");
                contentVo.setCover(coverUrls[coverUrls.length-1]);
            }
            messageVo.setData(contentVo);
        }
        message.setData(JSON.toJSONString(messageVo.getData()));

        //存储消息
        WebsocketUtil.messageService.save(message);
        messageVo.setId(message.getId());

        if (messageVo.getType().equals("private")){
            WebsocketUtil.sendToUser(messageVo.getToUserId(),messageVo);
            WebsocketUtil.sendToUser(messageVo.getFromUserId(),messageVo);
        } else if (messageVo.getType().equals("attention")) {
            return;
        } else if (messageVo.getType().equals("system")) {
            return;
        } else if (messageVo.getType().equals("notification")) {
            return;
        }

    }

    public static void getDownlineTimeMessage(User user) {
        Date downlineTime = user.getDownlineTime();
        //每次重新连接websocket都要比较消息队列表里发布时间与用户最近离线时间
        //拉取前面未登录而错过的消息并剔除关注推送消息
        List<Message> messagesList = WebsocketUtil.messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .in(Message::getToUserId, user.getId(),0,-1).ge(Message::getPublicTime, downlineTime));
        if (messagesList.size() == 0) return;
        //获取信息中的idSet集合
        HashSet<Long> userIdSet = new HashSet<>();
        messagesList.stream().forEach(message -> {
            userIdSet.add(message.getFromUserId());
            userIdSet.add(message.getToUserId());
        });

        //获取userVo map集合
        Map<Long, UserVo> userVoMap = new HashMap<>();
        userMapper.selectBatchIds(userIdSet).forEach(u->{
            UserVo userVo = new UserVo();
            BeanUtils.copyProperties(u,userVo);
            userVo.setAvatar(WebsocketUtil.minioProperties.getUrl() + userVo.getAvatar());
            userVoMap.put(u.getId(),userVo);
        });

        //封装messageVo集合
        List<MessageVo> messageVoList = new ArrayList<>();
        messagesList.stream().forEach(message -> {
            MessageVo messageVo = new MessageVo();
            ContentVo contentVo = JSON.parseObject(message.getData(),ContentVo.class);
            contentVo.setCover(WebsocketUtil.minioProperties.getUrl() + contentVo.getCover());
            contentVo.setFileUrl(WebsocketUtil.minioProperties.getUrl() + contentVo.getFileUrl());
            BeanUtils.copyProperties(message,messageVo);
            messageVo.setData(contentVo);
            messageVo.setFromUserVo(userVoMap.get(messageVo.getFromUserId()));
            messageVo.setToUserVo(userVoMap.get(messageVo.getToUserId()));

            messageVoList.add(messageVo);
        });

        messageVoList.stream().forEach(messageVo -> {
            WebsocketUtil.sendToUser(user.getId(),messageVo);
        });
    }

    @Autowired
    public void setMessageService(MessageService messageService) {
        WebsocketUtil.messageService = messageService;
    }

    @Autowired
    public void setAttentionService(AttentionService attentionService) {
        WebsocketUtil.attentionService = attentionService;
    }

    @Autowired
    public void setAttentionMapper(AttentionMapper attentionMapper) {
        WebsocketUtil.attentionMapper = attentionMapper;
    }

    //添加用户id与用户信息对应关系
    public static void addUser(Long userId, UserVo userInfo) {
        if (userMap.get(userId) != null) {
            return;
        }
        WebsocketUtil.userMap.put(userId, userInfo);
    }

    //获取用户的信息
    public static UserVo getUser(Long userId) {
        return WebsocketUtil.userMap.get(userId);
    }

    //当用户断开ws连接时需要从容器中移除用户信息，避免内存溢出
    public static void removeUser(Long userId) {
        if (WebsocketUtil.userMap.get(userId) == null) return;
        WebsocketUtil.userMap.remove(userId);
    }

    //添加用户id与session的对应关系
    public static void addSession(Long userId, Session session) {
        if (WebsocketUtil.sessionMap.get(userId) != null) return;
        WebsocketUtil.sessionMap.put(userId, session);
    }

    //获取用户的session信息
    public static Session getSession(Long userId) {
        return WebsocketUtil.sessionMap.get(userId);
    }

    //当用户断开ws连接时候需要从容器中移除session，避免内存溢出
    public static void removeSession(Long userId) {
        if (WebsocketUtil.sessionMap.get(userId) == null) return;
        WebsocketUtil.sessionMap.remove(userId);
    }

    //构造与用户交换的消息体
    public static MessageVo buildMessageVo(Message message) {
        MessageVo messageVo = new MessageVo();
        BeanUtils.copyProperties(message, messageVo);
        messageVo.setData(JSON.parseObject(message.getData(), ContentVo.class));
        return messageVo;
    }

    //构造与数据库交互的消息体
    public static Message buildMessage(MessageVo messageVo) {
        Message message = new Message();
        BeanUtils.copyProperties(messageVo, message);
        message.setData(JSONObject.toJSONString(messageVo.getData()));
        return message;
    }

    /**
     * 存储消息
     *
     * @param messageVo
     */
    public static void storeMessage(MessageVo messageVo) {
        Message message = buildMessage(messageVo);
        messageService.save(message);
    }

    /**
     * 发送消息
     *
     * @param userId
     * @param message
     */
    public static void sendMsg(Long userId, Message message) {
        Session session = WebsocketUtil.getSession(userId);
        if (null != session) {
            session.getAsyncRemote().sendText(JSON.toJSONString(message, SerializerFeature.DisableCircularReferenceDetect));//异步发送消息
        }
    }


    public static boolean isUserOnline(Long receiverId) {
        Session session = WebsocketUtil.getSession(receiverId);
        return session != null ? true : false;
    }

    /**
     * 发送信息
     *
     * @param toUserId
     * @param messageVo
     */
    public static void sendToUser(Long toUserId, MessageVo messageVo) {
        Session session = sessionMap.get(toUserId);
        if (null != session) {
            session.getAsyncRemote().sendText(JSON.toJSONString(messageVo, SerializerFeature.DisableCircularReferenceDetect));
        }
    }

}
