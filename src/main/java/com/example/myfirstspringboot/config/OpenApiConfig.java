package com.example.myfirstspringboot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) 配置类
 * <p>
 * 配置 API 文档的基本信息，包括标题、版本、描述等
 * 访问地址：http://localhost:8080/swagger-ui/index.html
 * </p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * 配置 OpenAPI 文档基本信息
     *
     * @return OpenAPI 配置对象
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("职位跟踪看板 API")
                        .version("v1.0.0")
                        .description("职位跟踪看板后端服务接口文档<br/><br/>" +
                                "<b>技术栈：</b>Spring Boot 3 + MyBatis + PostgreSQL<br/>" +
                                "<b>功能：</b>看板管理、列管理、卡片管理（CRUD、移动、软删除）")
                        .contact(new Contact()
                                .name("开发团队")
                                .email("dev@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
