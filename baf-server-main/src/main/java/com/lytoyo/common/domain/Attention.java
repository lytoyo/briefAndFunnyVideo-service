package com.lytoyo.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Package:com.lytoyo.common.domain
 *
 * @ClassName:Attention
 * @Create:2026/1/7 9:17
 **/
@Data
@TableName("attention")
public class Attention extends BaseEntity{

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 贴主id
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 粉丝id
     */
    @TableField("fans_user_id")
    private Long fansUserId;

    /**
     * 时间戳
     */
    @TableField("timestamp")
    private Long timestamp;

    /**
     * 状态 1-关注 0-取关
     */
    @TableField("status")
    private Integer status;


}
