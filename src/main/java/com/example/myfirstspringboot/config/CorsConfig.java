package com.example.myfirstspringboot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * 跨域配置类
 *
 * <p>
 * 配置全局 CORS 规则，允许前端应用跨域访问后端 API
 * </p>
 */
@Configuration
public class CorsConfig {

    @Value("${CORS_ALLOWED_ORIGIN_1:http://localhost:3000}")
    private String corsOrigin1;

    @Value("${CORS_ALLOWED_ORIGIN_2:https://www.hezhuoyao.top}")
    private String corsOrigin2;

    @Value("${CORS_ALLOWED_ORIGIN_3:https://hezhuoyao.top}")
    private String corsOrigin3;

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
        
        List<String> allowedOrigins = Arrays.asList(corsOrigin1, corsOrigin2, corsOrigin3);
        config.setAllowedOrigins(allowedOrigins);
        
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
