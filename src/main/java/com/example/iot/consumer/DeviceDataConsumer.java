package com.example.iot.consumer;

import com.example.iot.config.IotProperties;
import com.example.iot.model.DeviceReading;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Phase 4 Kafka 消费服务。
 *
 * <p>该组件负责消费 `device-data` topic 中的设备数据，
 * 并将处理后的结果分别写入 InfluxDB 和 Redis：
 * 1. InfluxDB 用于时间序列历史查询
 * 2. Redis 用于快速读取设备最新值
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceDataConsumer {

    private static final String INFLUX_MEASUREMENT = "device_metrics";

    private final IotProperties iotProperties;
    private final InfluxDBClient influxDBClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 消费 Kafka 消息并写入下游存储。
     *
     * <p>如果消息解析失败或某个存储写入失败，当前实现只记录日志，
     * 不继续向上抛出异常，避免阻塞后续消费链路。</p>
     *
     * @param payload Kafka 中的 DeviceReading JSON
     */
    @KafkaListener(
            topics = "${iot.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id:iot-consumer-group}"
    )
    public void onMessage(String payload) {
        try {
            // 先统一解析成领域对象，后续两个存储都复用这份结构化数据。
            DeviceReading reading = objectMapper.readValue(payload, DeviceReading.class);

            writeToInfluxDb(reading);
            writeToRedis(reading);

            log.info("Device data consumed successfully: deviceId={}, topic={}",
                    reading.getDeviceId(), iotProperties.getKafka().getTopic());
        } catch (Exception e) {
            log.error("Failed to consume Kafka message: {} - {}. Message: {}",
                    e.getClass().getSimpleName(), e.getMessage(), payload);
        }
    }

    /**
     * 将设备数据写入 InfluxDB 时间序列。
     *
     * <p>measurement 固定为 `device_metrics`，
     * `deviceId` 作为 tag，温度和转速作为 field。</p>
     */
    void writeToInfluxDb(DeviceReading reading) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();

            // 如果上游没有给 timestamp，则兜底使用当前时间，避免写入失败。
            Instant timestamp = reading.getTimestamp() != null ? reading.getTimestamp() : Instant.now();

            Point point = Point.measurement(INFLUX_MEASUREMENT)
                    .addTag("deviceId", reading.getDeviceId())
                    .addField("temperature", reading.getTemperature())
                    .addField("rpm", reading.getRpm())
                    .time(timestamp, WritePrecision.NS);

            writeApi.writePoint(point);

            log.debug("InfluxDB write successful: deviceId={}, temperature={}, rpm={}",
                    reading.getDeviceId(), reading.getTemperature(), reading.getRpm());
        } catch (Exception e) {
            log.error("Failed to write data to InfluxDB: {} - {}, deviceId={}",
                    e.getClass().getSimpleName(), e.getMessage(), reading.getDeviceId());
        }
    }

    /**
     * 将设备最新值写入 Redis，并设置 TTL。
     *
     * <p>Redis key 格式：
     * `device:latest:{deviceId}` 或测试环境中的自定义前缀。</p>
     */
    void writeToRedis(DeviceReading reading) {
        try {
            String key = iotProperties.getRedis().getKeyPrefix() + reading.getDeviceId();
            String value = objectMapper.writeValueAsString(reading);
            Duration ttl = Duration.ofSeconds(iotProperties.getRedis().getTtlSeconds());

            // 使用带 TTL 的 set，保证 Redis 只缓存最新短期状态。
            stringRedisTemplate.opsForValue().set(key, value, ttl);

            log.debug("Redis write successful: key={}, ttlSeconds={}",
                    key, iotProperties.getRedis().getTtlSeconds());
        } catch (Exception e) {
            log.error("Failed to write data to Redis: {} - {}, deviceId={}",
                    e.getClass().getSimpleName(), e.getMessage(), reading.getDeviceId());
        }
    }
}
