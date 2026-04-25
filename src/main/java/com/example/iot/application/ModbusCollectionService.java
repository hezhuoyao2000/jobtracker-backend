package com.example.iot.application;

import com.example.iot.config.IotProperties;
import com.example.iot.domain.DeviceReading;
import com.example.iot.infrastructure.modbus.ModbusDeviceReader;
import com.example.iot.infrastructure.mqtt.MqttDeviceReadingPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Modbus 采集应用服务。
 *
 * <p>该服务编排“检查连接 -> 读取 Modbus -> 发布 MQTT”的一次采集流程，
 * 调度入口只需要调用 initialize 或 collectOnce。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "iot.modbus", name = "mock-enabled", havingValue = "false", matchIfMissing = true)
public class ModbusCollectionService {

    private static final int MAX_RETRY = 3;

    private final IotProperties iotProperties;
    private final ModbusDeviceReader modbusDeviceReader;
    private final MqttDeviceReadingPublisher mqttDeviceReadingPublisher;
    private final AtomicInteger retryCount = new AtomicInteger(0);

    /**
     * 应用就绪后按配置决定是否立即建立 Modbus 连接。
     */
    public void initialize() {
        if (!iotProperties.getModbus().isEnabled()) {
            log.info("Modbus gateway is disabled (iot.modbus.enabled=false)");
            return;
        }
        modbusDeviceReader.connect();
    }

    /**
     * 执行一次设备采集和 MQTT 发布。
     */
    public void collectOnce() {
        if (!iotProperties.getModbus().isEnabled()) {
            return;
        }

        if (!modbusDeviceReader.isConnected()) {
            int retry = retryCount.incrementAndGet();
            if (retry <= MAX_RETRY) {
                log.warn("Modbus not connected, attempting reconnect {}/{}", retry, MAX_RETRY);
                modbusDeviceReader.connect();
            } else {
                log.error("Modbus reconnect failed after {} attempts, giving up", MAX_RETRY);
                retryCount.set(0);
            }
            return;
        }

        try {
            DeviceReading reading = modbusDeviceReader.read();
            if (reading != null) {
                mqttDeviceReadingPublisher.publish(reading);
                retryCount.set(0);
            }
        } catch (Exception e) {
            log.error("Error polling Modbus data: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            modbusDeviceReader.markDisconnected();
        }
    }

    /**
     * 返回当前重连次数，便于单元测试验证调度行为。
     */
    public int getRetryCount() {
        return retryCount.get();
    }

    /**
     * 重置重连计数，避免测试之间互相污染。
     */
    public void resetRetryCount() {
        retryCount.set(0);
    }
}
