package com.lytoyo.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

/**
 * Package:com.lytoyo.common.domain
 *
 * @ClassName:Blog
 * @Create:2025/12/22 11:23
 **/
@Data
@TableName("blog")
@Document(indexName = "blog")
@Accessors(chain = true)
public class Blog extends BaseEntity{

    private static final long serialVersionUID = 1L;

    @Id
    @TableId(type = IdType.AUTO)
    private Long id;

    @Field(type = FieldType.Long)
    @TableField("user_id")
    private Long userId;

    @Field(type = FieldType.Text,analyzer = "ik_max_word")
    @TableField("content")
    private String content;

    @Field(type = FieldType.Keyword)
    @TableField("file_name")
    private String fileName;

    @Field(type = FieldType.Keyword)
    @TableField("file_type")
    private String fileType;

    @Field(type = FieldType.Keyword)
    @TableField("cover")
    private String cover;

    @Field(type = FieldType.Keyword)
    @TableField("tag")
    private String tag;

    @Field(type = FieldType.Integer)
    @TableField("status")
    private Integer status;

    @Field(type = FieldType.Integer)
    @TableField("view_count")
    private Integer viewCount;

    @Field(type = FieldType.Integer)
    @TableField("like_count")
    private Integer likeCount;

    @Field(type = FieldType.Integer)
    @TableField("comment_count")
    private Integer commentCount;

    @Field(type = FieldType.Integer)
    @TableField("collect_count")
    private Integer collectCount;

    @Field(type = FieldType.Date,
            format = DateFormat.custom,
            pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("publish_time")
    private Date publishTime;

}
