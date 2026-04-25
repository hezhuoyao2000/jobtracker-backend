package com.example.iot.application;

import com.example.iot.domain.DeviceReading;
import com.example.iot.infrastructure.mqtt.MqttDeviceReadingPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock Modbus 采集应用服务。
 *
 * <p>用于测试和本地开发环境生成模拟设备数据，保持下游 MQTT/Kafka/Redis
 * 链路与真实采集模式一致。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "iot.modbus", name = "mock-enabled", havingValue = "true")
public class MockModbusCollectionService {

    private final MqttDeviceReadingPublisher mqttDeviceReadingPublisher;
    private long messageCount = 0;

    /**
     * 生成一条模拟设备读数并发布到 MQTT。
     */
    public void generateOnce() {
        try {
            DeviceReading reading = createRandomReading();
            mqttDeviceReadingPublisher.publish(reading);
            messageCount++;

            if (messageCount % 10 == 0) {
                log.info("Mock gateway sent {} messages, last: temp={}C, rpm={}",
                        messageCount, reading.getTemperature(), reading.getRpm());
            }
        } catch (Exception e) {
            log.error("Mock gateway error: {}", e.getMessage());
        }
    }

    /**
     * 按真实寄存器取值范围生成模拟读数，保证测试数据格式接近生产链路。
     */
    private DeviceReading createRandomReading() {
        int rawTemp = ThreadLocalRandom.current().nextInt(600, 1001);
        int rawRpm = ThreadLocalRandom.current().nextInt(1000, 3001);

        return DeviceReading.builder()
                .deviceId("device-001")
                .temperature(rawTemp / 10.0)
                .rpm(rawRpm)
                .timestamp(Instant.now())
                .rawTemperature(rawTemp)
                .rawRpm(rawRpm)
                .build();
    }

    /**
     * 获取已发布消息数，用于集成测试验证调度链路。
     */
    public long getMessageCount() {
        return messageCount;
    }

    /**
     * 重置计数器，避免测试之间互相影响。
     */
    public void resetMessageCount() {
        messageCount = 0;
    }
}
