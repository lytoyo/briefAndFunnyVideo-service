package com.lytoyo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lytoyo.common.domain.FileInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * Package:com.lytoyo.framework.mapper
 *
 * @ClassName:FileMapper
 * @Create:2025/12/22 10:49
 **/
@Mapper
public interface FileMapper extends BaseMapper<FileInfo> {
}
