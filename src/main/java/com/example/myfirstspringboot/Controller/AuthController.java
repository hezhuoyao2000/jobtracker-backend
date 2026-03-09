package com.example.myfirstspringboot.Controller;

import com.example.myfirstspringboot.dto.request.LoginRequest;
import com.example.myfirstspringboot.dto.request.RegisterRequest;
import com.example.myfirstspringboot.dto.response.AuthResponse;
import com.example.myfirstspringboot.exception.ApiResponse;
import com.example.myfirstspringboot.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * <p>
 * 处理用户登录、注册等认证相关请求
 * </p>
 *
 * @author yourname
 * @since 2026-03-08
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户认证相关接口：登录、注册等")
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     *
     * <p>
     * 接口说明：
     * - 使用用户名和密码进行登录验证
     * - 验证成功后返回 JWT Token
     * - 如果用户没有看板，自动创建默认看板
     * - 密码使用 BCrypt 加密存储和验证
     * </p>
     *
     * @param request 登录请求参数
     * @return JWT Token 和当前看板信息
     */
    @Operation(
            summary = "用户登录",
            description = "使用用户名和密码登录。验证成功后返回 JWT Token 和当前看板信息。如果用户没有看板，会自动创建默认看板。"
    )
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(
            @Parameter(description = "登录请求参数", required = true)
            @RequestBody LoginRequest request) {

        log.info("收到登录请求：username={}", request.getUsername());
        AuthResponse response = authService.login(request);
        log.info("登录成功：userId={}, username={}, boardId={}",
                response.getUserId(), response.getUsername(), response.getCurrentBoard().getBoardId());
        return ApiResponse.success(response);
    }

    /**
     * 用户注册
     *
     * <p>
     * 接口说明：
     * - 使用用户名和密码进行注册
     * - 密码使用 BCrypt 加密存储
     * - 注册成功后自动创建默认看板
     * - 返回 JWT Token 和初始看板信息
     * </p>
     *
     * @param request 注册请求参数
     * @return JWT Token 和初始看板信息
     */
    @Operation(
            summary = "用户注册",
            description = "使用用户名和密码注册。用户名唯一，密码长度至少 6 位。注册成功后自动创建默认看板并返回 JWT Token。"
    )
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(
            @Parameter(description = "注册请求参数", required = true)
            @RequestBody RegisterRequest request) {

        log.info("收到注册请求：username={}", request.getUsername());
        AuthResponse response = authService.register(request);
        log.info("注册成功：userId={}, username={}, boardId={}",
                response.getUserId(), response.getUsername(), response.getCurrentBoard().getBoardId());
        return ApiResponse.success(response);
    }
}
