package com.example.iot.gateway;

import com.example.iot.config.IotProperties;
import com.example.iot.model.DeviceReading;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.procimg.Register;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Modbus 采集网关。
 *
 * <p>负责按调度周期读取 Modbus TCP 寄存器，并将采集结果发布到 MQTT。
 * MQTT 改为发布前按需连接，避免部署时 Broker 暂不可达拖垮整个应用。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "iot.modbus", name = "mock-enabled", havingValue = "false", matchIfMissing = true)
public class ModbusGatewayService {

    private static final int MAX_RETRY = 3;

    private final IotProperties iotProperties;
    private final MqttClient mqttClient;
    private final ObjectMapper objectMapper;

    private ModbusTCPMaster modbusMaster;
    private final AtomicInteger retryCount = new AtomicInteger(0);

    /**
     * 应用就绪后按配置决定是否立即建立 Modbus 连接。
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
     * 建立 Modbus TCP 连接。
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
     * 按配置周期轮询设备数据；连接断开时尝试在后续调度周期补连。
     */
    @Scheduled(fixedDelayString = "${iot.modbus.poll-interval-ms:1000}")
    public void pollDeviceData() {
        if (!iotProperties.getModbus().isEnabled()) {
            return;
        }

        if (modbusMaster == null || !modbusMaster.isConnected()) {
            int retry = retryCount.incrementAndGet();
            if (retry <= MAX_RETRY) {
                log.warn("Modbus not connected, attempting reconnect {}/{}", retry, MAX_RETRY);
                connectModbus();
            } else {
                log.error("Modbus reconnect failed after {} attempts, giving up", MAX_RETRY);
                retryCount.set(0);
            }
            return;
        }

        try {
            DeviceReading reading = readModbusData();
            if (reading != null) {
                publishToMqtt(reading);
                retryCount.set(0);
            }
        } catch (Exception e) {
            log.error("Error polling Modbus data: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            modbusMaster = null;
        }
    }

    /**
     * 读取两个 Holding Registers，并按既定格式转换为领域对象。
     */
    private DeviceReading readModbusData() throws Exception {
        var config = iotProperties.getModbus();
        int slaveId = config.getSlaveId();

        Register[] registers = modbusMaster.readMultipleRegisters(slaveId, 0, 2);

        if (registers == null || registers.length < 2) {
            log.warn("Invalid Modbus response: expected 2 registers, got {}",
                    registers == null ? "null" : registers.length);
            return null;
        }

        int rawTemperature = registers[0].getValue();
        int rawRpm = registers[1].getValue();

        double temperature = rawTemperature / 10.0;
        int rpm = rawRpm;

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
     * 发布采集结果到 MQTT；若 Broker 尚未就绪，则仅记录告警并等待下一轮采集继续尝试。
     */
    private void publishToMqtt(DeviceReading reading) {
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
     * 在真正发布前再尝试建立 MQTT 连接，避免 Broker 短暂不可达时阻塞应用启动。
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
     * 统一使用配置中的连接参数，避免运行期补连和初始配置出现偏差。
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

    // ============ 测试辅助 ============

    /**
     * 注入测试专用 ModbusTCPMaster，用于隔离真实网络依赖。
     */
    void setModbusMaster(ModbusTCPMaster modbusMaster) {
        this.modbusMaster = modbusMaster;
    }

    /**
     * 返回当前重连次数，便于验证失败后的调度行为。
     */
    int getRetryCount() {
        return retryCount.get();
    }

    /**
     * 重置重连计数，避免测试之间互相污染。
     */
    void resetRetryCount() {
        retryCount.set(0);
    }
}
