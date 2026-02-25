package com.lytoyo.service;

import com.lytoyo.common.domain.Result;
import com.lytoyo.common.domain.User;

/**
 * Package:com.lytoyo.service
 *
 * @ClassName:SystemService
 * @Create:2025/12/2 9:30
 **/
public interface SystemService {
    /**
     * 获取邮箱验证码
     * @param email
     * @return Result
     */
    Result getCode(String email);

    /**
     * 注册账户
     * @param user
     * @return Result
     */
    Result register(User user);

    /**
     * 用户登录
     * @param user
     * @return
     */
    Result login(User user);

    /**
     * 退出登录
     * @param userId
     * @return
     */
    Result logoff(Long userId);
}
