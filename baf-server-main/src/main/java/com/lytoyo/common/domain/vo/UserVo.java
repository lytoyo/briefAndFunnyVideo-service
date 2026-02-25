package com.lytoyo.common.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.lytoyo.common.domain.BaseEntity;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Package:com.lytoyo.common.domain.vo
 *
 * @ClassName:UserVo
 * @Create:2025/11/25 17:56
 **/
@Data
@Document(indexName = "user")
public class UserVo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    /**
     * 用户类型(0-超级管理员 1-管理员 2-普通用户,默认为2)
     */
    @Field(type = FieldType.Keyword)
    private Integer category;

    /**
     * 用户昵称
     */
    @Field(type = FieldType.Text,analyzer = "ik_max_word")
    private String userName;

    /**
     * 性别(1-男生 2-女生 默认为1)
     */
    @Field(type = FieldType.Integer)
    private Integer sex;

    /**
     * 头像
     */
    @Field(type = FieldType.Keyword)
    private String avatar;

    /**
     * 状态： 0-禁用 1-启用 2-锁定
     */
    @Field(type = FieldType.Integer)
    private Integer status;

    /**
     * 简介
     */
    @Field(type = FieldType.Keyword)
    private String intro;

    /**
     * 省份
     */
    @Field(type = FieldType.Keyword)
    private String province;

    /**
     * 点赞数量
     */
    @Field(type = FieldType.Long)
    private Long liked;
}
