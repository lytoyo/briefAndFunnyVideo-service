package com.lytoyo.common.utils;
import java.util.Objects;
import java.util.stream.Stream;
/**
 * @author: lytoyo
 * @description: ResultCodeEnum
 * @date: 2025/12/2
 */
public enum ResultCodeEnum {
    SUCCESS(200, "操作成功"),
    FAIL(400, "操作失败"),
    UNAUTHORIZED(401, "未授权"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    INTERNAL_SERVER_ERROR(500,"服务响应失败");
 
 
    private Integer code;            // 状态码
    private String message;          // 消息
 
    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
 
    public Integer getCode() {
        return this.code;
    }
 
    public String getMessage() {
        return this.message;
    }
 
    public static ResultCodeEnum valueof(Integer code) {
        return Stream.of(values())
                .filter(option -> Objects.equals(option.code, code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No matching enum value for [" + code + "]"));
    }
 
}