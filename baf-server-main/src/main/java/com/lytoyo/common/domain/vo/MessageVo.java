package com.lytoyo.common.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Package:com.lytoyo.common.domain.vo
 *
 * @ClassName:MessageVo
 * @Create:2026/1/6 15:14
 **/
@Data
public class MessageVo implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String type;                    //消息类型：heartbeat, private, attention，system, notification
    private Long fromUserId;                //发送者id
    private Long toUserId;                  //接收者id
    private Date publicTime;                 //发送时间
    private UserVo fromUserVo;              //发送者用户详情
    private UserVo toUserVo;                //接收者用户详情
    private Long p2pId;                     //私聊Id
    // 消息体
    private ContentVo data;
}
