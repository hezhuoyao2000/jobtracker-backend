package com.example.myfirstspringboot.Controller;

import com.example.myfirstspringboot.Entity.User;
import com.example.myfirstspringboot.exception.ApiResponse;
import com.example.myfirstspringboot.repository.UserRepository;
import com.example.myfirstspringboot.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

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

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    /**
     * 用户登录（简化版）
     *
     * <p>
     * 接口说明：
     * - 简化登录流程，仅验证 userId 是否存在（不为空）
     * - 实际生产环境应验证用户名密码
     * - 登录成功后返回 JWT Token
     * </p>
     *
     * @param request 登录请求参数
     * @return JWT Token
     */
    @Operation(
            summary = "用户登录",
            description = "简化登录接口，传入 userId 即可获取 JWT Token。如果用户不存在会自动创建。"
    )
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Parameter(description = "登录请求参数", required = true)
            @RequestBody LoginRequest request) {

        // 简化验证：userId 不能为空
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            return ApiResponse.error(400, "用户 ID 不能为空");
        }

        String userId = request.getUserId().trim();
        log.info("用户登录: userId={}", userId);

        // 检查用户是否存在，不存在则自动创建
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.info("用户不存在，自动创建: userId={}", userId);
            User newUser = User.builder()
                    .id(userId)
                    .username(userId)
                    .displayName(userId)
                    .build();
            userRepository.save(newUser);
        }

        // 生成 JWT Token
        String token = jwtUtil.generateToken(userId);

        LoginResponse response = new LoginResponse();
        response.setUserId(userId);
        response.setToken(token);
        response.setTokenType("Bearer");

        log.info("登录成功: userId={}", userId);
        return ApiResponse.success(response);
    }

    /**
     * 登录请求
     */
    @Data
    public static class LoginRequest {
        /**
         * 用户 ID
         */
        private String userId;
    }

    /**
     * 用户注册
     *
     * <p>
     * 接口说明：
     * - 简化注册流程，仅需用户名即可注册
     * - 实际生产环境应要求密码、邮箱等
     * - 注册成功后返回 JWT Token
     * </p>
     *
     * @param request 注册请求参数
     * @return JWT Token
     */
    @Operation(
            summary = "用户注册",
            description = "简化注册接口，传入用户名即可注册并获取 JWT Token。"
    )
    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(
            @Parameter(description = "注册请求参数", required = true)
            @RequestBody RegisterRequest request) {

        // 参数验证
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            return ApiResponse.error(400, "用户名不能为空");
        }

        String username = request.getUsername().trim();

        // 检查用户名是否已存在
        if (userRepository.existsByUsername(username)) {
            return ApiResponse.error(409, "用户名已存在");
        }

        log.info("用户注册: username={}", username);

        // 创建新用户
        String userId = UUID.randomUUID().toString();
        User newUser = User.builder()
                .id(userId)
                .username(username)
                .displayName(request.getDisplayName() != null ? request.getDisplayName() : username)
                .email(request.getEmail())
                .build();

        userRepository.save(newUser);
        log.info("用户注册成功: userId={}, username={}", userId, username);

        // 生成 JWT Token
        String token = jwtUtil.generateToken(userId);

        RegisterResponse response = new RegisterResponse();
        response.setUserId(userId);
        response.setUsername(username);
        response.setToken(token);
        response.setTokenType("Bearer");

        return ApiResponse.success(response);
    }

    /**
     * 登录响应
     */
    @Data
    public static class LoginResponse {
        /**
         * 用户 ID
         */
        private String userId;

        /**
         * JWT Token
         */
        private String token;

        /**
         * Token 类型
         */
        private String tokenType;
    }

    /**
     * 注册请求
     */
    @Data
    public static class RegisterRequest {
        /**
         * 用户名（必填）
         */
        private String username;

        /**
         * 显示名称（可选）
         */
        private String displayName;

        /**
         * 邮箱（可选）
         */
        private String email;
    }

    /**
     * 注册响应
     */
    @Data
    public static class RegisterResponse {
        /**
         * 用户 ID
         */
        private String userId;

        /**
         * 用户名
         */
        private String username;

        /**
         * JWT Token
         */
        private String token;

        /**
         * Token 类型
         */
        private String tokenType;
    }
}
