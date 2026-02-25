package com.lytoyo.common.domain.vo;

import com.lytoyo.common.domain.BaseEntity;
import com.lytoyo.common.domain.Comment;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Package:com.lytoyo.common.domain.vo
 *
 * @ClassName:BlogVo
 * @Create:2026/1/8 16:54
 **/
@Data
@Accessors(chain = true)
public class BlogVo extends BaseEntity {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long userId;
    private Integer userCategory;
    private String userName;
    private Integer userStatus;
    private String userAvatar;
    private String content;
    private String cover;
    private String fileName;
    private String fileType;
    private String url;
    private Integer status;
    private BigDecimal duration;
    private Integer likeCount;
    private Integer viewCount;
    private Boolean isLiked;
    private Boolean isCollect;
    private Integer commentCount;
    private Integer collectCount;
    private Date publishTime;
    private List<Comment> commentList;
    private Integer commentCurrent;
    private Integer commentPages;
}
