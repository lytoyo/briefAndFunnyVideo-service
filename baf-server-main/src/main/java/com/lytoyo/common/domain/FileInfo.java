package com.lytoyo.common.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * Package:com.lytoyo.common.domain
 *
 * @ClassName:File
 * @Create:2025/12/22 10:41
 **/
@Data
@TableName
@Accessors(chain = true)
public class FileInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("file_name")
    private String fileName;

    @TableField("size")
    private Long size;

    @TableField("type")
    private String type;

    @TableField("duration")
    private BigDecimal duration;

    @TableField("width")
    private Integer width;

    @TableField("height")
    private Integer height;

    @TableField("suffix")
    private String suffix;

    @TableField("url")
    private String url;

    @TableField("description")
    private String description;
}
