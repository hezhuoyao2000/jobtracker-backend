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
    private KafkaProperties kafka;
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
        private ConsumerProperties consumer = new ConsumerProperties();

        @Data
        public static class ConsumerProperties {
            private boolean enabled = true;
            private String clientId = "java-ingestion-001";
            private String topic = "devices/data";
            private int qos = 1;
        }
    }

    @Data
    public static class KafkaProperties {
        private String topic = "device-data";
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
        /**
         * Redis Pub/Sub channel 名称。
         *
         * <p>用途：用于 SSE “推送出口”。DeviceDataConsumer 会把写入 Redis 的同一份 JSON 发布到该 channel。
         */
        private String pubsubChannel = "iot:device-data";
    }
}
