package com.example.iot.infrastructure.mqtt;

import com.example.iot.config.IotProperties;
import com.example.iot.domain.DeviceReading;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Component;

/**
 * 设备读数 MQTT 发布适配器。
 *
 * <p>该类只负责把领域对象序列化并发布到配置中的 MQTT topic，连接失败时
 * 保持降级，不让外部 Broker 的短暂不可用中断采集任务。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttDeviceReadingPublisher {

    private final IotProperties iotProperties;
    private final MqttClient mqttClient;
    private final ObjectMapper objectMapper;

    /**
     * 发布设备读数到 MQTT。
     *
     * @param reading 已转换好的设备读数
     */
    public void publish(DeviceReading reading) {
        try {
            if (!ensureMqttConnected()) {
                log.warn("Skip MQTT publish because broker is still unavailable");
                return;
            }

            String topic = iotProperties.getMqtt().getTopic();
            String payload = objectMapper.writeValueAsString(reading);

            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(iotProperties.getMqtt().getQos());

            mqttClient.publish(topic, message);
            log.debug("Published to MQTT [{}]: {}", topic, payload);
        } catch (Exception e) {
            log.error("Failed to publish to MQTT: {} - {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * 发布前按需连接 MQTT，避免应用启动阶段强依赖外部 Broker。
     */
    private boolean ensureMqttConnected() {
        if (mqttClient.isConnected()) {
            return true;
        }

        try {
            mqttClient.connect(buildConnectOptions());
            log.info("MQTT publisher connected successfully");
            return true;
        } catch (Exception e) {
            log.warn("Failed to connect MQTT publisher: {} - {}",
                    e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    /**
     * 复用 application.yml 中的连接配置，保证首次连接和重连行为一致。
     */
    private MqttConnectOptions buildConnectOptions() {
        var config = iotProperties.getMqtt();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(config.getConnectionTimeout());
        options.setKeepAliveInterval(config.getKeepAliveInterval());
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        return options;
    }
}
