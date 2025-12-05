package com.lytoyo.common.domain;
 
import com.lytoyo.common.constant.SystemConstant;
import com.lytoyo.common.exception.BaseErrorInfoInterface;
import com.lytoyo.common.utils.ResultCodeEnum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @author: Rehse
 * @description: Result
 * @date: 2025/8/14
 */
@Data
@Builder
@AllArgsConstructor
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;
    private T data;
 
    public Result(){
    }
    public Result(ResultCodeEnum codeEnum) {
        this.code = codeEnum.getCode();
        this.msg = codeEnum.getMessage();
    }
 
    public Result(ResultCodeEnum codeEnum, T data) {
        this.code = codeEnum.getCode();
        this.msg = codeEnum.getMessage();
        this.data = data;
    }
 
    public static <T> Result<T> success() {
        Result<T> response = new Result<>();
        response.setCode(ResultCodeEnum.SUCCESS.getCode());
        response.setMsg(SystemConstant.SUCCESS);
        return response;
    }
 
    // 用得最多的就是这个
    public static <T> Result<T> success(T data) {
        Result<T> response = new Result<>();
        response.setCode(ResultCodeEnum.SUCCESS.getCode());
        response.setMsg(SystemConstant.SUCCESS);
        response.setData(data);
        return response;
    }
 
    public static <T> Result<T> fail(String msg) {
        Result<T> response = new Result<>();
        response.setCode(ResultCodeEnum.FAIL.getCode());
        response.setMsg(msg);
        return response;
    }
 
    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> response = new Result<>();
        response.setCode(code);
        response.setMsg(message);
        return response;
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> response = new Result<>();
        response.setCode(code);
        response.setMsg(message);
        return response;
    }

    public static <T> Result<T> error(BaseErrorInfoInterface errorInfo) {
        Result<T> response = new Result<>();
        response.setCode(Integer.parseInt(errorInfo.getResultCode()));
        response.setMsg(errorInfo.getResultMsg());
        response.setData(null);
        return response;
    }


}