package com.example.iot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * IoT 模块专用测试启动类
 * 用于集成测试，只加载 IoT 相关组件
 */
@SpringBootApplication(scanBasePackages = {"com.example.iot"})
@EnableScheduling
public class IotTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(IotTestApplication.class, args);
    }
}
