package com.example.myfirstspringboot.dto.request;

import lombok.Data;

/**
 * 注册请求 DTO
 */
@Data
public class RegisterRequest {
    /**
     * 用户名（必填）
     * - 唯一，不能重复
     * - 长度建议 3-20 字符
     */
    private String username;

    /**
     * 密码（必填）
     * - 长度至少 6 位
     * - 建议包含字母和数字
     */
    private String password;

    /**
     * 显示名称（可选）
     * - 不传则使用 username
     */
    private String displayName;

    /**
     * 邮箱（可选）
     */
    private String email;
}
