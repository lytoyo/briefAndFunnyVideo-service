package com.lytoyo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lytoyo.common.domain.User;
import com.lytoyo.common.utils.PasswordUtil;
import com.lytoyo.mapper.UserMapper;
import com.lytoyo.service.UserService;
import io.minio.MinioClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * Package:com.lytoyo.service.impl
 *
 * @ClassName:UserService
 * @Create:2025/12/1 9:32
 **/

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper,User> implements UserService {
    @Resource
    private PasswordUtil passwordUtil;

    @Resource
    private UserMapper userMapper;

    @Resource
    private MinioClient minioClient;

    /**
     * 用户注册
     */
    public User register(String username, String plainPassword) {
        User user = new User();
        user.setUserName(username);

        // 1. 生成随机盐值
        String salt = passwordUtil.generateSalt();
        user.setSalt(salt);

        // 2. 加密密码（密码+盐值）
        String encryptedPassword = passwordUtil.encryptPassword(plainPassword, salt);
        user.setPassword(encryptedPassword);

         this.save(user);

         
         return user;

    }

    /**
     * 用户登录验证
     */
    public boolean login(String email, String inputPassword) {
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) return false;

        // 使用存储的盐值验证密码
        return passwordUtil.verifyPassword(inputPassword, user.getPassword(), user.getSalt());
    }
}
