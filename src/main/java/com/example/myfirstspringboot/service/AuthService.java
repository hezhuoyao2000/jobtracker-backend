package com.example.myfirstspringboot.service;

import com.example.myfirstspringboot.dto.request.RegisterRequest;
import com.example.myfirstspringboot.dto.request.LoginRequest;
import com.example.myfirstspringboot.dto.response.AuthResponse;

/**
 * 认证服务接口
 * <p>
 * 处理用户登录、注册等认证相关业务逻辑
 * </p>
 */
public interface AuthService {

    /**
     * 用户登录
     * <p>
     * 验证用户名和密码，生成 JWT Token，确保用户有看板
     * </p>
     *
     * @param request 登录请求参数
     * @return 认证响应（包含用户信息、Token 和看板信息）
     */
    AuthResponse login(LoginRequest request);

    /**
     * 用户注册
     * <p>
     * 创建新用户，加密密码，生成 JWT Token，创建默认看板
     * </p>
     *
     * @param request 注册请求参数
     * @return 认证响应（包含用户信息、Token 和看板信息）
     */
    AuthResponse register(RegisterRequest request);
}
