package com.example.iot.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * InfluxDB 客户端配置
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class InfluxDbConfig {

    private final IotProperties iotProperties;

    @Bean
    public InfluxDBClient influxDBClient() {
        var config = iotProperties.getInfluxdb();
        log.info("Initializing InfluxDB client: url={}, org={}, bucket={}",
                config.getUrl(), config.getOrg(), config.getBucket());

        InfluxDBClient client = InfluxDBClientFactory.create(
                config.getUrl(),
                config.getToken().toCharArray(),
                config.getOrg(),
                config.getBucket()
        );

        // 测试连接
        try {
            var health = client.health();
            if (health.getStatus() == com.influxdb.client.domain.HealthCheck.StatusEnum.PASS) {
                log.info("InfluxDB connected successfully");
            } else {
                log.warn("InfluxDB health check failed: {}", health.getMessage());
            }
        } catch (Exception e) {
            log.warn("InfluxDB connection test failed: {}", e.getMessage());
        }

        return client;
    }
}
