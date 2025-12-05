package com.lytoyo.common.utils;

import com.lytoyo.common.domain.vo.UserVo;

public class AuthContextHolder {
    private static ThreadLocal<UserVo> userVo = new ThreadLocal<UserVo>();

    private static ThreadLocal<Long> userId = new ThreadLocal<Long>();

    public static void setUserId(Long _userId) {
        userId.set(_userId);
    }

    public static Long getUserId() {
        return userId.get();
    }

    public static void removeUserId() {
        userId.remove();
    }

    public static void setUserVo(UserVo _userVo) {
        userVo.set(_userVo);
    }

    public static UserVo getUserVo() {
        return userVo.get();
    }

    public static void removeUserVo() {
        userVo.remove();
    }

}