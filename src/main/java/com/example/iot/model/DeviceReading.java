package com.example.iot.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 设备读数数据模型
 * 用于在Modbus、MQTT、Kafka、InfluxDB、Redis之间传递数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceReading {

    /** 设备ID，例如 device-001 */
    private String deviceId;

    /** 温度值，单位 °C */
    private double temperature;

    /** 转速值，单位 RPM */
    private int rpm;

    /** 时间戳（ISO-8601格式） */
    private Instant timestamp;

    /** 原始温度值（用于调试） */
    private Integer rawTemperature;

    /** 原始转速值（用于调试） */
    private Integer rawRpm;

    /**
     * 获取当前时间戳的便捷方法
     */
    public static DeviceReading create(String deviceId, double temperature, int rpm) {
        return DeviceReading.builder()
                .deviceId(deviceId)
                .temperature(temperature)
                .rpm(rpm)
                .timestamp(Instant.now())
                .build();
    }
}
