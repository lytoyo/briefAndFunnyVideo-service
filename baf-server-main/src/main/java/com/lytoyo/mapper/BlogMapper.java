package com.lytoyo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lytoyo.common.domain.Blog;
import org.apache.ibatis.annotations.Mapper;

/**
 * Package:com.lytoyo.framework.mapper
 *
 * @ClassName:BlogMapper
 * @Create:2025/12/22 11:30
 **/
@Mapper
public interface BlogMapper extends BaseMapper<Blog> {
}
