package com.example.iot.application;

import com.example.iot.config.IotProperties;
import com.example.iot.domain.DeviceReading;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * MQTT 设备消息接入应用服务。
 *
 * <p>负责把 MQTT payload 解析为设备读数，并按 deviceId 作为 key 转发到 Kafka。
 * MQTT 回调入口本身放在 entrypoint.listener 包中。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MqttIngestionService {

    private final IotProperties iotProperties;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 处理 MQTT 到达消息。
     *
     * @param topic MQTT topic
     * @param payload 原始 JSON payload
     */
    public void ingest(String topic, String payload) {
        if (log.isDebugEnabled()) {
            log.debug("MQTT message arrived on topic {}: {}", topic, payload);
        }

        try {
            DeviceReading reading = objectMapper.readValue(payload, DeviceReading.class);
            sendToKafka(reading, payload);
        } catch (Exception e) {
            log.error("Failed to process MQTT message: {} - {}. Message: {}",
                    e.getClass().getSimpleName(), e.getMessage(), payload);
        }
    }

    /**
     * 使用 deviceId 作为 Kafka key，保证同一设备尽量落在相同分区。
     */
    private void sendToKafka(DeviceReading reading, String originalPayload) {
        String topic = iotProperties.getKafka().getTopic();
        String key = reading.getDeviceId();

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
}
