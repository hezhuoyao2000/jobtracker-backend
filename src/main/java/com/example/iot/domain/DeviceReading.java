package com.example.iot.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 设备读数领域模型。
 *
 * <p>该对象作为 IoT 采集链路的统一数据载体，在 Modbus、MQTT、Kafka、
 * InfluxDB、Redis 和 SSE 推送之间传递。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceReading {

    /** 设备唯一标识，例如 device-001。 */
    private String deviceId;

    /** 设备温度值，单位摄氏度。 */
    private double temperature;

    /** 设备转速值，单位 RPM。 */
    private int rpm;

    /** 读数产生时间，使用 ISO-8601 可序列化格式。 */
    private Instant timestamp;

    /** 原始温度寄存器值，用于采集链路调试和数据核对。 */
    private Integer rawTemperature;

    /** 原始转速寄存器值，用于采集链路调试和数据核对。 */
    private Integer rawRpm;

    /**
     * 快速创建不包含原始寄存器值的设备读数。
     *
     * @param deviceId 设备唯一标识
     * @param temperature 温度值
     * @param rpm 转速值
     * @return 带当前时间戳的设备读数
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
