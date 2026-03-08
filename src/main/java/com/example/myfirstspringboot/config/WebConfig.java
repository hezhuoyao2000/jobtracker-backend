package com.example.myfirstspringboot.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CorsFilter;

/**
 * Web 配置类
 *
 * <p>
 * 注册 JWT 过滤器等 Web 组件
 * </p>
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsFilter corsFilter;

    /**
     * 注册 CORS 过滤器
     *
     * <p>
     * 顺序设置为 0，确保在 JWT 过滤器之前执行，处理 OPTIONS 预检请求
     * </p>
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(corsFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(0);
        return registration;
    }

    /**
     * 注册 JWT 过滤器
     *
     * <p>
     * 顺序设置为 1，确保在 CORS 过滤器之后执行
     * </p>
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilter() {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(jwtAuthenticationFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }
}
