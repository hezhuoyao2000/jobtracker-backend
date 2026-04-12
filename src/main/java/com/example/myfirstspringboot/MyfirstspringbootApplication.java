package com.example.myfirstspringboot;

import com.example.iot.config.IotProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot 应用启动类
 */
@SpringBootApplication
@EnableScheduling
@EnableKafka
@ComponentScan(basePackages = {"com.example.myfirstspringboot", "com.example.iot"})
@EnableConfigurationProperties(IotProperties.class)
public class MyfirstspringbootApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyfirstspringbootApplication.class, args);
    }

}
