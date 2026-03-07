package com.example.myfirstspringboot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;

/**
 * 应用启动后自动打开浏览器
 * <p>
 * 在应用启动成功后，自动打开系统默认浏览器访问 Swagger UI 文档页面
 * 仅在开发环境启用，可通过配置关闭
 * </p>
 */
@Slf4j
@Component
public class BrowserLauncher implements ApplicationRunner {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${app.auto-open-browser:true}")
    private boolean autoOpenBrowser;

    @Value("${app.swagger-path:/swagger-ui/index.html}")
    private String swaggerPath;

    @Override
    public void run(ApplicationArguments args) {
        if (!autoOpenBrowser) {
            log.info("自动打开浏览器已禁用，可通过 http://localhost:{}{} 手动访问 API 文档",
                    serverPort, swaggerPath);
            return;
        }

        String swaggerUrl = "http://localhost:" + serverPort + swaggerPath;

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                log.info("正在打开浏览器访问 API 文档: {}", swaggerUrl);
                Desktop.getDesktop().browse(new URI(swaggerUrl));
            } else {
                log.warn("当前系统不支持自动打开浏览器，请手动访问: {}", swaggerUrl);
            }
        } catch (Exception e) {
            log.error("打开浏览器失败: {}", e.getMessage());
            log.info("请手动访问 API 文档: {}", swaggerUrl);
        }
    }
}
