package com.example.iot.entrypoint.listener;

import com.example.iot.application.MqttIngestionService;
import com.example.iot.config.IotProperties;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MQTT 设备数据监听入口。
 *
 * <p>该入口负责维护 MQTT Consumer 连接和订阅，并把到达的消息交给应用服务。
 * 连接维护注解集中在这里，便于识别后台监听任务。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "iot.mqtt.consumer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MqttDeviceDataListener implements MqttCallback {

    private final IotProperties iotProperties;
    private final MqttClient mqttConsumerClient;
    private final MqttIngestionService mqttIngestionService;
    private final AtomicBoolean subscribed = new AtomicBoolean(false);

    /**
     * 初始化 MQTT 回调并立即尝试连接订阅。
     */
    @PostConstruct
    public void init() {
        var consumerConfig = iotProperties.getMqtt().getConsumer();

        if (!consumerConfig.isEnabled() || mqttConsumerClient == null) {
            log.info("MQTT Ingestion listener is disabled");
            return;
        }

        mqttConsumerClient.setCallback(this);
        ensureConnectedAndSubscribed();
    }

    /**
     * 容器关闭前主动断开 MQTT 连接，避免遗留会话。
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
     * 周期性恢复 MQTT Consumer 连接与订阅。
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
     * MQTT 连接意外断开时重置订阅状态，等待下一轮维护任务恢复。
     */
    @Override
    public void connectionLost(Throwable cause) {
        log.error("MQTT Consumer connection lost: {} - {}",
                cause.getClass().getSimpleName(), cause.getMessage());
        subscribed.set(false);
    }

    /**
     * MQTT 消息到达入口。
     *
     * @param topic MQTT topic
     * @param message MQTT message
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        mqttIngestionService.ingest(topic, new String(message.getPayload()));
    }

    /**
     * Consumer 场景不处理 deliveryComplete，保留空实现满足接口契约。
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Consumer listener does not publish MQTT messages.
    }

    /**
     * 确保 Consumer 已连接并订阅目标 topic。
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
                log.info("MQTT Ingestion listener subscribed to topic={} with qos={}",
                        consumerConfig.getTopic(), consumerConfig.getQos());
            }
        } catch (MqttException e) {
            log.warn("MQTT Consumer connect/subscribe attempt failed: {} - {}",
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * 使用当前配置构造 MQTT Consumer 连接参数。
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
