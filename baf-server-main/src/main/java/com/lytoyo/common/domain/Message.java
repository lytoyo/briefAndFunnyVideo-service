package com.lytoyo.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

/**
 * Package:com.lytoyo.common.domain
 *
 * @ClassName:Message
 * @Create:2026/1/5 13:46
 **/
@Data
@TableName("message")
public class Message extends BaseEntity{

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 消息类型：heartbeat, private, attention，system, notification
     */
    @TableField("type")
    private String type;

    /**
     * 发送时间
     */
    @TableField("public_time")
    private Date publicTime;

    /**
     * 发送者id
     */
    @TableField("from_user_id")
    private Long fromUserId;

    /**
     * 接收者id
     */
    @TableField("to_user_id")
    private Long toUserId;

    /**
     * 私聊id
     */
    @TableField("p2p_id")
    private Long p2pId;

    /**
     * 消息内容
     */
    @TableField("data")
    private String data;

}
