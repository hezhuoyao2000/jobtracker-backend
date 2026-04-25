package com.example.myfirstspringboot.config;

import com.example.myfirstspringboot.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 *
 * <p>
 * 拦截请求，验证 JWT Token 的有效性，并将 userId 存入请求属性中
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    /**
     * 不需要验证 Token 的路径
     */
    private static final String[] WHITELIST_PATHS = {
            "/auth/login",
            "/auth/register",
            "/swagger-ui",
            "/v3/api-docs",
            "/swagger-resources",
            "/webjars",
            "/actuator",
            "/error",
            "/iot"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestPath = request.getRequestURI();

        // 白名单路径直接放行
        if (isWhitelisted(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 提取 Authorization Header
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("请求缺少 Authorization Header 或格式错误: {}", requestPath);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未提供有效的认证令牌\",\"data\":null}");
            return;
        }

        // 提取 Token
        String token = authHeader.substring(7);

        // 验证 Token
        if (!jwtUtil.validateToken(token)) {
            log.warn("Token 验证失败: {}", requestPath);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"认证令牌无效或已过期\",\"data\":null}");
            return;
        }

        // 提取 userId 并存入请求属性
        String userId = jwtUtil.extractUserId(token);
        request.setAttribute("userId", userId);
        log.debug("用户认证成功: userId={}, path={}", userId, requestPath);

        filterChain.doFilter(request, response);
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhitelisted(String requestPath) {
        for (String path : WHITELIST_PATHS) {
            if (requestPath.startsWith(path)) {
                return true;
            }
        }
        return false;
    }
}
