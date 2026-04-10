package com.lytoyo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lytoyo.common.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;


/**
 * Package:com.lytoyo.mapper
 *
 * @ClassName:UserMapper
 * @Create:2025/12/1 9:28
 **/
@Mapper
public interface UserMapper extends BaseMapper<User>  {

    @Select("SELECT COUNT(*) as new_users \n" +
            "FROM user \n" +
            "WHERE create_time >= CURDATE() \n" +
            "  AND create_time < DATE_ADD(CURDATE(), INTERVAL 1 DAY);")
    long selectAddUserCount();

}
