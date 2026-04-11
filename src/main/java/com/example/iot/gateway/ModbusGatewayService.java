package com.example.iot.gateway;

import com.example.iot.config.IotProperties;
import com.example.iot.model.DeviceReading;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Modbus 网关服务
 * 从 Modbus TCP 读取设备数据，发布到 MQTT
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "iot.modbus", name = "mock-enabled", havingValue = "false", matchIfMissing = true)
public class ModbusGatewayService {

    private final IotProperties iotProperties;
    private final MqttClient mqttClient;
    private final ObjectMapper objectMapper;

    private ModbusTCPMaster modbusMaster;
    private final AtomicInteger retryCount = new AtomicInteger(0);
    private static final int MAX_RETRY = 3;

    /**
     * 应用启动后初始化 Modbus 连接
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (!iotProperties.getModbus().isEnabled()) {
            log.info("Modbus gateway is disabled (iot.modbus.enabled=false)");
            return;
        }
        connectModbus();
    }

    /**
     * 建立 Modbus TCP 连接
     */
    private void connectModbus() {
        var config = iotProperties.getModbus();
        try {
            log.info("Connecting to Modbus TCP: {}:{}, slaveId={}",
                    config.getHost(), config.getPort(), config.getSlaveId());

            modbusMaster = new ModbusTCPMaster(config.getHost(), config.getPort());
            modbusMaster.connect();

            retryCount.set(0);
            log.info("Modbus TCP connected successfully");
        } catch (Exception e) {
            log.error("Failed to connect Modbus TCP: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            modbusMaster = null;
        }
    }

    /**
     * 定时轮询读取 Modbus 数据
     * 间隔通过 iot.modbus.poll-interval-ms 配置（默认 1000ms）
     */
    @Scheduled(fixedDelayString = "${iot.modbus.poll-interval-ms:1000}")
    public void pollDeviceData() {
        if (!iotProperties.getModbus().isEnabled()) {
            return;
        }

        // 检查连接，如果断开则尝试重连
        if (modbusMaster == null || !modbusMaster.isConnected()) {
            int retry = retryCount.incrementAndGet();
            if (retry <= MAX_RETRY) {
                log.warn("Modbus not connected, attempting reconnect {}/{}", retry, MAX_RETRY);
                connectModbus();
            } else {
                log.error("Modbus reconnect failed after {} attempts, giving up", MAX_RETRY);
                // 重置计数器，下次调度会再次尝试
                retryCount.set(0);
            }
            return;
        }

        try {
            DeviceReading reading = readModbusData();
            if (reading != null) {
                publishToMqtt(reading);
                // 成功读取后重置重试计数
                retryCount.set(0);
            }
        } catch (Exception e) {
            log.error("Error polling Modbus data: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            // 连接可能已断开，下次调度会触发重连
            modbusMaster = null;
        }
    }

    /**
     * 从 Modbus 读取数据
     * 读取寄存器 0 和 1（温度、转速）
     */
    private DeviceReading readModbusData() throws Exception {
        var config = iotProperties.getModbus();
        int slaveId = config.getSlaveId();

        // 读取 2 个 Holding Registers，从地址 0 开始
        Register[] registers = modbusMaster.readMultipleRegisters(slaveId, 0, 2);

        if (registers == null || registers.length < 2) {
            log.warn("Invalid Modbus response: expected 2 registers, got {}",
                    registers == null ? "null" : registers.length);
            return null;
        }

        // 解析数据
        int rawTemperature = registers[0].getValue();  // 地址 0: 温度原始值
        int rawRpm = registers[1].getValue();          // 地址 1: 转速原始值

        // 单位换算
        double temperature = rawTemperature / 10.0;    // 原始值 / 10 = °C
        int rpm = rawRpm;                              // 直接为 RPM

        return DeviceReading.builder()
                .deviceId("device-001")
                .temperature(temperature)
                .rpm(rpm)
                .timestamp(Instant.now())
                .rawTemperature(rawTemperature)
                .rawRpm(rawRpm)
                .build();
    }

    /**
     * 将数据发布到 MQTT
     */
    private void publishToMqtt(DeviceReading reading) {
        try {
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

    // ============ 测试辅助方法 ============

    /**
     * 设置 ModbusTCPMaster（用于测试）
     */
    void setModbusMaster(ModbusTCPMaster modbusMaster) {
        this.modbusMaster = modbusMaster;
    }

    /**
     * 获取当前重试计数（用于测试）
     */
    int getRetryCount() {
        return retryCount.get();
    }

    /**
     * 重置重试计数（用于测试）
     */
    void resetRetryCount() {
        retryCount.set(0);
    }
}
