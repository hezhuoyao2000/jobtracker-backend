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
 * MQTT Consumer 客户端配置
 * 独立的 Consumer Client，不与 Gateway 的 Publisher 复用
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MqttConsumerConfig {

    private final IotProperties iotProperties;

    /**
     * MQTT Consumer Client Bean
     * 用于 Phase 3: MQTT → Kafka 消息接入
     */
    @Bean
    public MqttClient mqttConsumerClient() throws MqttException {
        var consumerConfig = iotProperties.getMqtt().getConsumer();
        var mqttConfig = iotProperties.getMqtt();

        if (!consumerConfig.isEnabled()) {
            log.info("MQTT Consumer is disabled");
            return null;
        }

        log.info("Initializing MQTT Consumer client: broker={}, clientId={}",
                mqttConfig.getBroker(), consumerConfig.getClientId());

        MqttClient client = new MqttClient(
                mqttConfig.getBroker(),
                consumerConfig.getClientId(),
                new MemoryPersistence()
        );

        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(mqttConfig.getConnectionTimeout());
        options.setKeepAliveInterval(mqttConfig.getKeepAliveInterval());
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);

        // 不在 Spring 启动阶段强连 Broker，Consumer 由运行期自恢复任务负责连接和订阅。
        log.info("MQTT Consumer client initialized without eager connect; subscription will be established on demand");

        return client;
    }
}
