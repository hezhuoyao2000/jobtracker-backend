package com.example.iot.entrypoint.scheduler;

import com.example.iot.application.MockModbusCollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Mock Modbus 数据生成调度入口。
 *
 * <p>该入口仅在 iot.modbus.mock-enabled=true 时启用，用来替代真实 Modbus
 * 采集任务。</p>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "iot.modbus", name = "mock-enabled", havingValue = "true")
public class MockModbusPollingTask {

    private final MockModbusCollectionService mockModbusCollectionService;

    /**
     * 按采集周期生成一条模拟数据。
     */
    @Scheduled(fixedDelayString = "${iot.modbus.poll-interval-ms:1000}")
    public void generateMockData() {
        mockModbusCollectionService.generateOnce();
    }
}
