package com.example.myfirstspringboot.config;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 配置类
 * 扫描 mapper 包下的所有接口
 */
@Configuration
@MapperScan("com.example.myfirstspringboot.mapper")
public class MyBatisConfig {

    // MyBatis 配置已在 application.yaml 中完成
    // 此类主要用于扩展配置（如需要）
    // UuidTypeHandler 已使用 @MappedTypes 注解，会自动注册
}
