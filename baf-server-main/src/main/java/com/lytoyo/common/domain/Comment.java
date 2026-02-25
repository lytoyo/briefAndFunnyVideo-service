package com.lytoyo.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * Package:com.lytoyo.common.domain
 *
 * @ClassName:comment
 * @Create:2026/1/7 9:42
 **/
@Data
@TableName("comment")
public class Comment extends BaseEntity{

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 帖子id
     */
    @TableField("post_id")
    private Long postId;

    /**
     * 评论者id
     */
    @TableField("comment_user_id")
    private Long commentUserId;

    /**
     * 父级评论id 如果本身就是父级评论，则为0
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 评论内容，可能为纯文字，可能图文或视频文
     */
    @TableField("comment")
    private String comment;

    /**
     * 评论的文件类型
     */
    @TableField("file_type")
    private String fileType;

    /**
     * 评论文件名
     */
    @TableField("file_name")
    private String fileName;

    @TableField("cover")
    private String cover;

    /**
     * 点赞数量
     */
    @TableField("liked_count")
    private Long likedCount;

    /**
     *  发布时间
     */
    @TableField("public_time")
    private Date publicTime;
}
