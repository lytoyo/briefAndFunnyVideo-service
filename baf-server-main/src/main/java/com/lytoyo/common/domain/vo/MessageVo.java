package com.lytoyo.common.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * Package:com.lytoyo.common.domain.vo
 *
 * @ClassName:MessageVo
 * @Create:2026/1/6 15:14
 **/
@Data
public class MessageVo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private Integer messageTypeCode;        // 使用消息类型
    private Integer contentTypeCode;        // 消息内容类型
    private Long timestamp;                 //时间戳
    private Long fromUserId;                //发送者id
    private Long toUserId;                  //接收者id
    // 消息体
    private Object data;
}
