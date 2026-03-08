package com.example.myfirstspringboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置类
 *
 * <p>
 * 配置全局 CORS 规则，允许前端应用跨域访问后端 API
 * </p>
 */
@Configuration
public class CorsConfig {

    /**
     * 配置 CORS 过滤器
     *
     * <p>
     * 该过滤器需要在 JWT 过滤器之前执行，确保 OPTIONS 预检请求能被正确处理
     * </p>
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许前端地址访问（开发环境）
        config.addAllowedOrigin("http://localhost:3000");
        // 允许所有 HTTP 方法（GET, POST, PUT, DELETE, OPTIONS 等）
        config.addAllowedMethod("*");
        // 允许所有请求头
        config.addAllowedHeader("*");
        // 允许携带凭证（如 Cookie、Authorization Header）
        config.setAllowCredentials(true);
        // 预检请求缓存时间（秒）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径生效
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
