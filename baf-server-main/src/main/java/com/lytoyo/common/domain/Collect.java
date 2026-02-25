package com.lytoyo.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Package:com.lytoyo.common.domain
 *
 * @ClassName:Collect
 * @Create:2026/1/12 10:30
 **/
@Data
@TableName("collect")
public class Collect extends BaseEntity{

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 贴子id
     */
    @TableField("post_id")
    private Long postId;

    /**
     * 收藏用户id
     */
    @TableField("collect_user_id")
    private Long collectUserId;

    /**
     * 时间戳
     */
    @TableField("timestamp")
    private Long timestamp;

    /**
     * 收藏状态，0-取消收藏 1-收藏
     */
    @TableField("status")
    private Integer status;
}
