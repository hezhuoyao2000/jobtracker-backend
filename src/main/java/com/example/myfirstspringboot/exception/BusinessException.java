package com.example.myfirstspringboot.exception;

import lombok.Getter;

/**
 * 业务异常类
 * <p>
 * 用于表示业务逻辑错误，如资源不存在、权限不足等
 * </p>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
