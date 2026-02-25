package com.lytoyo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lytoyo.common.domain.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * Package:com.lytoyo.mapper
 *
 * @ClassName:MessageMapper
 * @Create:2026/1/6 15:42
 **/
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
