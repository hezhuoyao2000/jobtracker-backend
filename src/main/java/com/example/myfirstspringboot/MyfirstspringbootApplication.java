package com.example.myfirstspringboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 应用启动类
 */
@SpringBootApplication
public class MyfirstspringbootApplication {

    public static void main(String[] args) {
        // SpringApplication.run() 会启动内嵌的 Tomcat 服务器并阻塞等待请求
        // 应用不会自动退出，除非手动停止或发生致命错误
        SpringApplication.run(MyfirstspringbootApplication.class, args);
    }

}
