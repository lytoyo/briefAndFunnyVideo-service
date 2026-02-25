package com.lytoyo.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * Package:com.lytoyo.common.domain
 *
 * @ClassName:User
 * @Create:2025/11/21 15:25
 **/
@Data
@TableName("user")
@Accessors(chain = true)
public class User extends BaseEntity{

    private static final long serialVersionUID = 1L;


    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 用户类型(0-超级管理员 1-管理员 2-普通用户,默认为2)
     */
    @TableField("category")
    private Integer category;

    /**
     * 密码
     */
    @TableField("password")
    private String password;

    /**
     * 盐值
     */
    @TableField("salt")
    private String salt;

    /**
     * 用户昵称
     */
    @TableField("user_name")
    private String userName;

    /**
     * 性别(1-男生 2-女生 默认为1)
     */
    @TableField("sex")
    private Integer sex;

    /**
     * 头像
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 手机
     */
    @TableField("phone")
    private String phone;


    /**
     * 状态： 0-禁用 1-启用 2-锁定
     */
    @TableField("status")
    private Integer status;

    /**
     * 简介
     */
    @TableField("intro")
    private String intro;

    /**
     * 省份
     */
    @TableField("province")
    private String province;

    /**
     * 城市
     */
    @TableField("city")
    private String city;

    /**
     * 区
     */
    @TableField("area")
    private String area;

    /**
     * 详细地址
     */
    @TableField("address")
    private String address;

    /**
     * 最近登录时间
     */
    @TableField("loging_time")
    private Date logingTime;

    /**
     * 最近下线时间
     */
    @TableField("downline_time")
    private Date downlineTime;

}
