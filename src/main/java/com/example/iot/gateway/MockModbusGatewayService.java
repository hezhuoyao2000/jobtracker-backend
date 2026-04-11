package com.example.iot.gateway;

import com.example.iot.config.IotProperties;
import com.example.iot.model.DeviceReading;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock Modbus 网关服务
 * 用于测试环境，生成模拟设备数据，不依赖真实 Modbus 连接
 *
 * 启用方式：配置 iot.modbus.mock-enabled=true
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "iot.modbus", name = "mock-enabled", havingValue = "true")
public class MockModbusGatewayService {

    private final IotProperties iotProperties;
    private final MqttClient mqttClient;
    private final ObjectMapper objectMapper;

    // 计数器，用于统计发送的消息数
    private long messageCount = 0;

    /**
     * 定时生成模拟数据并发布到 MQTT
     */
    @Scheduled(fixedDelayString = "${iot.modbus.poll-interval-ms:1000}")
    public void generateMockData() {
        try {
            DeviceReading reading = createRandomReading();
            publishToMqtt(reading);
            messageCount++;

            if (messageCount % 10 == 0) {
                log.info("Mock gateway sent {} messages, last: temp={}°C, rpm={}",
                        messageCount, reading.getTemperature(), reading.getRpm());
            }
        } catch (Exception e) {
            log.error("Mock gateway error: {}", e.getMessage());
        }
    }

    /**
     * 生成随机设备读数
     * 温度范围: 600-1000 (原始值) → 60.0-100.0°C
     * 转速范围: 1000-3000 RPM
     */
    private DeviceReading createRandomReading() {
        // 生成随机原始值
        int rawTemp = ThreadLocalRandom.current().nextInt(600, 1001);    // 600-1000
        int rawRpm = ThreadLocalRandom.current().nextInt(1000, 3001);    // 1000-3000

        // 转换为业务值
        double temperature = rawTemp / 10.0;
        int rpm = rawRpm;

        return DeviceReading.builder()
                .deviceId("device-001")
                .temperature(temperature)
                .rpm(rpm)
                .timestamp(Instant.now())
                .rawTemperature(rawTemp)
                .rawRpm(rawRpm)
                .build();
    }

    /**
     * 发布到 MQTT
     */
    private void publishToMqtt(DeviceReading reading) throws Exception {
        String topic = iotProperties.getMqtt().getTopic();
        String payload = objectMapper.writeValueAsString(reading);

        MqttMessage message = new MqttMessage(payload.getBytes());
        message.setQos(iotProperties.getMqtt().getQos());

        mqttClient.publish(topic, message);
        log.debug("Mock published to MQTT [{}]: {}", topic, payload);
    }

    /**
     * 获取已发送消息数（用于测试验证）
     */
    public long getMessageCount() {
        return messageCount;
    }

    /**
     * 重置计数器（用于测试）
     */
    public void resetMessageCount() {
        messageCount = 0;
    }
}
