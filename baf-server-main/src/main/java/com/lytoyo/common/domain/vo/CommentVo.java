package com.lytoyo.common.domain.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.lytoyo.common.domain.BaseEntity;
import lombok.Data;

import java.util.Date;

/**
 * Package:com.lytoyo.common.domain.vo
 *
 * @ClassName:CommentVo
 * @Create:2026/1/29 15:01
 **/
@Data
public class CommentVo extends BaseEntity {

    private Long id;

    /**
     * 帖子id
     */
    private Long postId;

    /**
     * 评论者id
     */
    private Long commentUserId;

    /**
     * 父级评论id 如果本身就是父级评论，则为0
     */
    private Long parentId;

    /**
     * 评论内容，可能为纯文字，可能图文或视频文
     */
    private String comment;

    /**
     * 评论的文件类型
     */
    private String fileType;

    /**
     * 评论文件名
     */
    private String fileName;

    /**
     * 视频封面
     */
    private String cover;

    /**
     * 评论点赞量
     */
    private Long likedCount;

    /**
     * 是否点赞
     */
    private Boolean isLiked;

    /**
     * 是否是贴主本人言论
     */
    private Boolean isPoster;

    /**
     * 多少条子评论
     */
    private Long childCommentCount;

    /**
     * 评论的用户信息
     */
    private UserVo userVo;

    /**
     *  发布时间
     */
    private Date publicTime;
}
