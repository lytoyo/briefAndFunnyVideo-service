package com.lytoyo.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Package:com.lytoyo.common.domain
 *
 * @ClassName:SysLog
 * @Create:2025/12/4 15:43
 **/
@Data
@TableName("sys_log")
public class SysLogBo {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 执行方法所在的类
     */
    @TableField("class_name")
    private String className;

    /**
     * 执行的方法名称
     */
    @TableField("method_name")
    private String methodName;

    /**
     * 方法参数
     */
    @TableField("params")
    private String params;

    /**
     * 执行所占时间
     */
    @TableField("exeu_time")
    private Long exeuTime;

    /**
     * 解释
     */
    @TableField("remark")
    private String remark;


    /**
     * 记录时间
     */
    @TableField("create_date")
    private String createDate;

}
