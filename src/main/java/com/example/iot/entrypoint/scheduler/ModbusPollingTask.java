package com.example.iot.entrypoint.scheduler;

import com.example.iot.application.ModbusCollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 真实 Modbus 采集调度入口。
 *
 * <p>该类只承载 Spring 启动事件和定时任务注解，具体采集流程交给应用服务执行，
 * 让后台任务入口像 Controller 一样集中可见。</p>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "iot.modbus", name = "mock-enabled", havingValue = "false", matchIfMissing = true)
public class ModbusPollingTask {

    private final ModbusCollectionService modbusCollectionService;

    /**
     * 应用启动完成后初始化 Modbus 连接。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        modbusCollectionService.initialize();
    }

    /**
     * 按配置周期触发一次 Modbus 采集。
     */
    @Scheduled(fixedDelayString = "${iot.modbus.poll-interval-ms:1000}")
    public void pollDeviceData() {
        modbusCollectionService.collectOnce();
    }
}
