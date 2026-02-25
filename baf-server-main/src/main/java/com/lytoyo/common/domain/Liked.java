package com.lytoyo.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Package:com.lytoyo.common.domain
 *
 * @ClassName:Liked
 * @Create:2026/1/7 9:35
 **/
@Data
@TableName("liked")
public class Liked extends BaseEntity{

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 点赞对象id
     */
    @TableField("obj_id")
    private Long objId;

    /**
     * 点赞类型，1-帖子点赞 2-评论点赞
     */
    @TableField("type")
    private Integer type;
    /**
     * 点赞用户id
     */
    @TableField("liked_user_id")
    private Long likedUserId;

    /**
     * 时间戳
     */
    @TableField("timestamp")
    private Long timestamp;

    /**
     * 点赞状态，0-取消点赞 1-点赞
     */
    @TableField("status")
    private Integer status;
}
