package com.lytoyo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lytoyo.common.domain.Comment;
import org.apache.ibatis.annotations.Mapper;

/**
 * Package:com.lytoyo.mapper
 *
 * @ClassName:CommentMapper
 * @Create:2026/1/7 9:54
 **/
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
