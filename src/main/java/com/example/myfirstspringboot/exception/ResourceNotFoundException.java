package com.example.myfirstspringboot.exception;

/**
 * 资源不存在异常
 * <p>
 * 当查询的资源（看板、列、卡片等）不存在时抛出
 * </p>
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s 不存在: %s = '%s'", resourceName, fieldName, fieldValue));
    }
}
