package com.example.iot.ingestion;

import com.example.iot.config.IotProperties;
import com.example.iot.model.DeviceReading;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * MQTT 消息接入服务
 * Phase 3: 订阅 MQTT Topic，将消息转发到 Kafka
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttIngestionService implements MqttCallback {

    private final IotProperties iotProperties;
    private final MqttClient mqttConsumerClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 初始化 MQTT 订阅
     */
    @PostConstruct
    public void init() {
        var consumerConfig = iotProperties.getMqtt().getConsumer();

        if (!consumerConfig.isEnabled() || mqttConsumerClient == null) {
            log.info("MQTT Ingestion Service is disabled");
            return;
        }

        try {
            // 设置回调
            mqttConsumerClient.setCallback(this);

            // 订阅 Topic
            String topic = consumerConfig.getTopic();
            int qos = consumerConfig.getQos();
            mqttConsumerClient.subscribe(topic, qos);

            log.info("MQTT Ingestion Service started: subscribed to topic={} with qos={}", topic, qos);
        } catch (MqttException e) {
            log.error("Failed to initialize MQTT Ingestion Service: {} - {}",
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * 清理资源
     */
    @PreDestroy
    public void cleanup() {
        if (mqttConsumerClient != null && mqttConsumerClient.isConnected()) {
            try {
                mqttConsumerClient.disconnect();
                log.info("MQTT Consumer client disconnected");
            } catch (MqttException e) {
                log.error("Error disconnecting MQTT Consumer: {} - {}",
                        e.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    /**
     * MQTT 连接断开回调
     */
    @Override
    public void connectionLost(Throwable cause) {
        log.error("MQTT Consumer connection lost: {} - {}",
                cause.getClass().getSimpleName(), cause.getMessage());
        // 自动重连由 MqttConnectOptions.setAutomaticReconnect(true) 处理
    }

    /**
     * MQTT 消息到达回调
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload());

        if (log.isDebugEnabled()) {
            log.debug("MQTT message arrived on topic {}: {}", topic, payload);
        }

        try {
            // 1. 反序列化 JSON
            DeviceReading reading = objectMapper.readValue(payload, DeviceReading.class);

            // 2. 发送到 Kafka
            sendToKafka(reading, payload);

        } catch (Exception e) {
            // JSON 解析失败或发送失败，记录错误但继续接收下一条
            log.error("Failed to process MQTT message: {} - {}. Message: {}",
                    e.getClass().getSimpleName(), e.getMessage(), payload);
        }
    }

    /**
     * 发送消息到 Kafka
     */
    private void sendToKafka(DeviceReading reading, String originalPayload) {
        String topic = iotProperties.getKafka().getTopic();
        String key = reading.getDeviceId(); // 使用 deviceId 作为 key，保证分区一致性

        try {
            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(topic, key, originalPayload);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Message sent to Kafka: topic={}, partition={}, offset={}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                } else {
                    log.error("Failed to send message to Kafka: topic={}, key={}, error={}",
                            topic, key, ex.getMessage());
                }
            });

        } catch (Exception e) {
            log.error("Exception sending to Kafka: {} - {}",
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * MQTT 消息发送完成回调（Consumer 不需要处理）
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Consumer 不需要处理发送完成回调
    }
}
