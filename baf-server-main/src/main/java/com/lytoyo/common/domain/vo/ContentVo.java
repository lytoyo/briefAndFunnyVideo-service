package com.lytoyo.common.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * Package:com.lytoyo.common.domain.vo
 *
 * @ClassName:ContentVo
 * @Create:2026/3/4 10:03
 **/
@Data
public class ContentVo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String content;
    private String fileType;
    private String fileUrl;
    private String cover;
}
