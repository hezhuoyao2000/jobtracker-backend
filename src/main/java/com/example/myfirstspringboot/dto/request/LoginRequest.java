package com.example.myfirstspringboot.dto.request;

import lombok.Data;

/**
 * 登录请求 DTO
 */
@Data
public class LoginRequest {
    /**
     * 用户名（必填）
     */
    private String username;

    /**
     * 密码（必填）
     */
    private String password;
}
