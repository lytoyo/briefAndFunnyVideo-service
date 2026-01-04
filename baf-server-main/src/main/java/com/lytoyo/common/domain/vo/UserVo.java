package com.lytoyo.common.domain.vo;

import com.lytoyo.common.domain.BaseEntity;
import lombok.Data;

/**
 * Package:com.lytoyo.common.domain.vo
 *
 * @ClassName:UserVo
 * @Create:2025/11/25 17:56
 **/
@Data
public class UserVo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 用户类型(0-超级管理员 1-管理员 2-普通用户,默认为2)
     */
    private Integer category;


    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 性别(1-男生 2-女生 默认为1)
     */
    private Integer sex;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 状态： 0-禁用 1-启用 2-锁定
     */
    private Integer status;

    /**
     * 简介
     */
    private String intro;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 区
     */
    private String area;

    /**
     * 详细地址
     */
    private String address;
}
