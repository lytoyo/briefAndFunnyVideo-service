package com.lytoyo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lytoyo.common.domain.Result;
import com.lytoyo.common.domain.User;

/**
 * Package:com.lytoyo.service
 *
 * @ClassName:AuthService
 * @Create:2025/12/1 9:24
 **/

public interface UserService extends IService<User> {

    /**
     * 获取用户状态等信息
     * @return
     */
    Result getStatus(Long userId);


    /**
     * 分页获取用户点赞帖子列表
     * @param current
     * @param size
     * @return
     */
    Result likedPostList(Long userId,Integer current,Integer size);

    /**
     * 分页获取关注用户列表
     * @param current
     * @param size
     * @return
     */
    Result attentionUserList(Long userId,Integer current, Integer size);

    /**
     * 分页获取粉丝用户列表
     * @param current
     * @param size
     * @return
     */
    Result fansUserList(Long userId,Integer current, Integer size);

    /**
     * 分页获取用户各项帖子列表
     * @param userId
     * @param current
     * @param size
     * @return
     */
    Result askPost(Long userId, Integer current, Integer size);

    /**
     * 获取用户发表的贴子
     * @param userId
     * @param current
     * @param size
     * @return
     */
    Result selfPostList(Long userId, Integer current, Integer size);

    /**
     * 获取用户收藏的贴子
     * @param userId
     * @param current
     * @param size
     * @return
     */
    Result collectPostList(Long userId, Integer current, Integer size);

    /**
     * 获取其他用户信息
     * @param userId
     * @return
     */
    Result otherUserDetail(Long userId);
}
