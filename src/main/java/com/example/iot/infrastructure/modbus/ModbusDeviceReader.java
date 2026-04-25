package com.example.iot.infrastructure.modbus;

import com.example.iot.config.IotProperties;
import com.example.iot.domain.DeviceReading;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.procimg.Register;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Modbus TCP 设备读取适配器。
 *
 * <p>该类封装 j2mod 的连接和寄存器读取细节，向应用层暴露稳定的
 * DeviceReading 读取能力。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "iot.modbus", name = "mock-enabled", havingValue = "false", matchIfMissing = true)
public class ModbusDeviceReader {

    private final IotProperties iotProperties;

    private ModbusTCPMaster modbusMaster;

    /**
     * 建立 Modbus TCP 连接。
     */
    public void connect() {
        var config = iotProperties.getModbus();
        try {
            log.info("Connecting to Modbus TCP: {}:{}, slaveId={}",
                    config.getHost(), config.getPort(), config.getSlaveId());

            modbusMaster = new ModbusTCPMaster(config.getHost(), config.getPort());
            modbusMaster.connect();

            log.info("Modbus TCP connected successfully");
        } catch (Exception e) {
            log.error("Failed to connect Modbus TCP: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            modbusMaster = null;
        }
    }

    /**
     * 判断当前 Modbus 连接是否可用。
     *
     * @return true 表示可以直接读取寄存器
     */
    public boolean isConnected() {
        return modbusMaster != null && modbusMaster.isConnected();
    }

    /**
     * 读取两个 Holding Registers，并转换为领域读数。
     *
     * @return 有效设备读数；响应不完整时返回 null
     */
    public DeviceReading read() throws Exception {
        var config = iotProperties.getModbus();
        Register[] registers = modbusMaster.readMultipleRegisters(config.getSlaveId(), 0, 2);

        if (registers == null || registers.length < 2) {
            log.warn("Invalid Modbus response: expected 2 registers, got {}",
                    registers == null ? "null" : registers.length);
            return null;
        }

        int rawTemperature = registers[0].getValue();
        int rawRpm = registers[1].getValue();

        return DeviceReading.builder()
                .deviceId("device-001")
                .temperature(rawTemperature / 10.0)
                .rpm(rawRpm)
                .timestamp(Instant.now())
                .rawTemperature(rawTemperature)
                .rawRpm(rawRpm)
                .build();
    }

    /**
     * 清空连接引用，让下一轮调度按重连流程恢复。
     */
    public void markDisconnected() {
        modbusMaster = null;
    }

    /**
     * 测试专用：注入 mock Modbus master，隔离真实网络依赖。
     */
    public void setModbusMaster(ModbusTCPMaster modbusMaster) {
        this.modbusMaster = modbusMaster;
    }
}
