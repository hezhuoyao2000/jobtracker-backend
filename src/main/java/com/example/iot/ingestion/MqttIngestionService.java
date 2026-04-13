package com.example.iot.ingestion;

import com.example.iot.config.IotProperties;
import com.example.iot.model.DeviceReading;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MQTT 消费与 Kafka 转发组件。
 *
 * <p>该组件负责订阅 MQTT 设备数据主题，并将原始消息可靠转发到 Kafka。
 * 为避免容器部署时 Broker 短暂不可达导致应用启动失败，连接与订阅改为运行期维护。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "iot.mqtt.consumer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MqttIngestionService implements MqttCallback {

    private final IotProperties iotProperties;
    private final MqttClient mqttConsumerClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean subscribed = new AtomicBoolean(false);

    /**
     * 初始化回调并立即尝试建立连接和订阅。
     */
    @PostConstruct
    public void init() {
        var consumerConfig = iotProperties.getMqtt().getConsumer();

        if (!consumerConfig.isEnabled() || mqttConsumerClient == null) {
            log.info("MQTT Ingestion Service is disabled");
            return;
        }

        mqttConsumerClient.setCallback(this);
        ensureConnectedAndSubscribed();
    }

    /**
     * 容器关闭前主动断开 MQTT 连接，避免留下脏会话。
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
     * 周期性维护 MQTT Consumer 连接与订阅，避免 Broker 在应用启动阶段暂不可达时整个链路失活。
     */
    @Scheduled(fixedDelayString = "${iot.mqtt.consumer.reconnect-interval-ms:5000}")
    public void maintainConnection() {
        var consumerConfig = iotProperties.getMqtt().getConsumer();
        if (!consumerConfig.isEnabled() || mqttConsumerClient == null) {
            return;
        }
        ensureConnectedAndSubscribed();
    }

    /**
     * 当 MQTT 连接意外断开时，重置订阅状态，等待定时任务或自动重连后恢复。
     */
    @Override
    public void connectionLost(Throwable cause) {
        log.error("MQTT Consumer connection lost: {} - {}",
                cause.getClass().getSimpleName(), cause.getMessage());
        subscribed.set(false);
    }

    /**
     * 处理 MQTT 到达消息，并转发到 Kafka。
     *
     * @param topic MQTT 主题
     * @param message MQTT 消息
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());

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
     * 使用 deviceId 作为 key 将原始消息发送到 Kafka。
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

    /**
     * Consumer 场景无需处理 deliveryComplete 回调，保留空实现。
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Consumer æ¶“å¶‰æ¸¶ç‘•ä½¸î˜©éžå——å½‚é–«ä½¸ç•¬éŽ´æ„¬æ´–ç’‹?
    }

    /**
     * 确保 Consumer 已连接到 Broker，且目标 Topic 的订阅已经恢复。
     */
    private void ensureConnectedAndSubscribed() {
        try {
            if (!mqttConsumerClient.isConnected()) {
                mqttConsumerClient.connect(buildConnectOptions());
                subscribed.set(false);
                log.info("MQTT Consumer client connected successfully");
            }

            if (!subscribed.get()) {
                var consumerConfig = iotProperties.getMqtt().getConsumer();
                mqttConsumerClient.subscribe(consumerConfig.getTopic(), consumerConfig.getQos());
                subscribed.set(true);
                log.info("MQTT Ingestion Service subscribed to topic={} with qos={}",
                        consumerConfig.getTopic(), consumerConfig.getQos());
            }
        } catch (MqttException e) {
            log.warn("MQTT Consumer connect/subscribe attempt failed: {} - {}",
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * 按当前配置构建连接参数，确保首次连接和重连行为保持一致。
     */
    private MqttConnectOptions buildConnectOptions() {
        var mqttConfig = iotProperties.getMqtt();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(mqttConfig.getConnectionTimeout());
        options.setKeepAliveInterval(mqttConfig.getKeepAliveInterval());
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        return options;
    }
}
