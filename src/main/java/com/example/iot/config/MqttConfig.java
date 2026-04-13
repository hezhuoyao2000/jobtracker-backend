package com.example.iot.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MQTT 客户端配置
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MqttConfig {

    private final IotProperties iotProperties;

    @Bean
    public MqttClient mqttClient() throws MqttException {
        var config = iotProperties.getMqtt();
        log.info("Initializing MQTT client: broker={}, clientId={}",
                config.getBroker(), config.getClientId());

        MqttClient client = new MqttClient(
                config.getBroker(),
                config.getClientId(),
                new MemoryPersistence()
        );

        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(config.getConnectionTimeout());
        options.setKeepAliveInterval(config.getKeepAliveInterval());
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);

        // 不在 Bean 初始化阶段强依赖外部 Broker，避免部署时因短暂网络不可达导致应用整体启动失败。
        log.info("MQTT client initialized without eager connect; connection will be established on demand");

        return client;
    }
}
