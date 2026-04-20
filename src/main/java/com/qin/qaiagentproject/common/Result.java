package com.qin.qaiagentproject.common;

import com.qin.qaiagentproject.exception.ErrorCode;
import lombok.Data;

/**
 * 统一响应结果类
 */
@Data
public class Result<T> {
    
    /**
     * 响应码
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    public Result() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 成功响应
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getDefaultMessage(), data);
    }
    
    /**
     * 成功响应（带消息）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }
    
    /**
     * 失败响应
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }
    
    /**
     * 失败响应（带错误码）
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 失败响应（错误码枚举）
     */
    public static <T> Result<T> error(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }
}

