package com.lytoyo.common.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Package:com.lytoyo.common.domain
 *
 * @ClassName:BaseEntity
 * @Create:2025/11/21 15:29
 **/
@Data
public class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否删除
     */
    @JsonIgnore
    @TableField("delete_flag")
    @TableLogic
    private Integer deleteFlag;

    @JsonIgnore
    @TableField(exist = false)
    private String searchValue;

    @TableField(fill = FieldFill.INSERT)
    private Date create_time;


    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date update_time;


    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params = new HashMap<>();

}
