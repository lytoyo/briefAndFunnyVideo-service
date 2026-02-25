package com.lytoyo.common.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lytoyo.common.domain.Attention;
import com.lytoyo.common.domain.Message;
import com.lytoyo.common.domain.vo.MessageVo;
import com.lytoyo.common.domain.vo.UserVo;
import com.lytoyo.mapper.AttentionMapper;
import com.lytoyo.service.AttentionService;
import com.lytoyo.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.websocket.Session;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        messageVo.setData(JSON.parse(message.getData()));
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
     * 根据信息类型将信息发送给对象
     *
     * @param messageVoList
     */
    public static void sendMessageToObject(List<MessageVo> messageVoList) {
        for (MessageVo messageVo : messageVoList) {
            //发送者
            Long fromUserId = messageVo.getFromUserId();
            //接收者
            Long toUserId = messageVo.getToUserId();
            switch (messageVo.getMessageTypeCode()) {
                //系统公告
                case 1:
                    //广播所有在线用户
                    for (Long userId : sessionMap.keySet()) {
                        sendToUser(userId, messageVo);
                    }
                    break;
                case 2:
                    //查看当前在线的粉丝
                    Set<Long> onLineUserIdList = sessionMap.keySet();
                    List<Long> onLineFansUserIdList = onLineUserIdList.stream()
                            .filter(attentionMapper.selectList(new LambdaQueryWrapper<Attention>()
                                    .eq(Attention::getUserId, messageVo.getFromUserId())
                                    .eq(Attention::getStatus, 1)).stream().map(attention -> {
                                    return attention.getFansUserId();
                            }).collect(Collectors.toList())::contains)
                            .collect(Collectors.toList());
                    for (Long userId: onLineFansUserIdList){
                        sendToUser(userId,messageVo);
                    }
                    break;
                //聊天、点赞、关注、评论消息
                default:
                    sendToUser(toUserId, messageVo);
                    break;
            }
        }

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
