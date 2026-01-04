package com.lytoyo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lytoyo.common.domain.User;
import org.apache.ibatis.annotations.Mapper;


/**
 * Package:com.lytoyo.mapper
 *
 * @ClassName:UserMapper
 * @Create:2025/12/1 9:28
 **/
@Mapper
public interface UserMapper extends BaseMapper<User>  {

}
