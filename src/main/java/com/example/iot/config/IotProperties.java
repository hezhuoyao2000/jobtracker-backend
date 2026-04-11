package com.example.iot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * IoT 模块配置属性
 * 绑定 application.yml 中 iot.* 配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "iot")
public class IotProperties {

    private ModbusProperties modbus;
    private MqttProperties mqtt;
    private InfluxDbProperties influxdb;
    private RedisProperties redis;

    @Data
    public static class ModbusProperties {
        private String host = "localhost";
        private int port = 502;
        private int slaveId = 1;
        private int pollIntervalMs = 1000;
        private boolean enabled = false;
        private boolean mockEnabled = false;  // Mock 模式开关
    }

    @Data
    public static class MqttProperties {
        private String broker = "tcp://localhost:1883";
        private String clientId = "java-gateway-001";
        private String topic = "devices/data";
        private int qos = 1;
        private int connectionTimeout = 10;
        private int keepAliveInterval = 60;
    }

    @Data
    public static class InfluxDbProperties {
        private String url = "http://localhost:8086";
        private String token = "my-super-secret-token";
        private String org = "iot-demo";
        private String bucket = "device-metrics";
    }

    @Data
    public static class RedisProperties {
        private String keyPrefix = "device:latest:";
        private int ttlSeconds = 30;
    }
}
