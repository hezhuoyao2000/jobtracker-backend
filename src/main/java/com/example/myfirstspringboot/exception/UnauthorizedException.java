package com.example.myfirstspringboot.exception;

/**
 * 权限不足异常
 * <p>
 * 当用户没有权限访问或操作某个资源时抛出
 * </p>
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String resourceName, String operation) {
        super(String.format("无权%s该%s", operation, resourceName));
    }
}
