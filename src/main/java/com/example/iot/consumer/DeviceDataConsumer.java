package com.example.iot.consumer;

import com.example.iot.config.IotProperties;
import com.example.iot.model.DeviceReading;
import com.example.iot.sse.SseEmitterManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Phase 4 Kafka 消费服务。
 *
 * <p>该组件负责消费 `device-data` topic 中的设备数据，
 * 并将处理后的结果分别写入 InfluxDB 和 Redis：
 * 1. InfluxDB 用于时间序列历史查询（可配置关闭）
 * 2. Redis 用于快速读取设备最新值
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceDataConsumer {

    private static final String INFLUX_MEASUREMENT = "device_metrics";

    private final IotProperties iotProperties;
    private final Optional<InfluxDBClient> influxDBClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final SseEmitterManager sseEmitterManager;

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

            // 写入 inluxdb
            writeToInfluxDb(reading);

            // 写入 redis
            String redisJson = writeToRedis(reading);

            // 4. 推送给 SSE 前端
            publishToRedisPubSubIfNeeded(redisJson);

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
        // 如果未启用 InfluxDB，则跳过写入
        if (!iotProperties.getInfluxdb().isEnabled()) {
            return;
        }

        try {
            WriteApiBlocking writeApi = influxDBClient.orElseThrow().getWriteApiBlocking();

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
    String writeToRedis(DeviceReading reading) {
        try {
            String key = iotProperties.getRedis().getKeyPrefix() + reading.getDeviceId();
            String value = objectMapper.writeValueAsString(reading);
            Duration ttl = Duration.ofSeconds(iotProperties.getRedis().getTtlSeconds());

            // 使用带 TTL 的 set，保证 Redis 只缓存最新短期状态。
            stringRedisTemplate.opsForValue().set(key, value, ttl);

            log.debug("Redis write successful: key={}, ttlSeconds={}",
                    key, iotProperties.getRedis().getTtlSeconds());
            return value;
        } catch (Exception e) {
            log.error("Failed to write data to Redis: {} - {}, deviceId={}",
                    e.getClass().getSimpleName(), e.getMessage(), reading.getDeviceId());
            return null;
        }
    }

    /**
     * 将 Redis “latest” 写入后的同一份 JSON 发布到 Redis Pub/Sub channel（用于 SSE 推送出口）。
     *
     * <p>按需求约束：如果没有任何 SSE 客户端连接，则不发布，做到“不触发就不发”。</p>
     *
     * @param json Redis value（DeviceReading JSON），可能为 null（写入失败或序列化失败）
     */
    void publishToRedisPubSubIfNeeded(String json) {
        if (json == null || json.isBlank()) {
            return;
        }
        if (!sseEmitterManager.hasClients()) {
            // 没有 SSE 连接时，不刷新“推送出口”（Redis channel）
            return;
        }

        try {
            String channel = iotProperties.getRedis().getPubsubChannel();
            stringRedisTemplate.convertAndSend(channel, json);
            log.debug("Redis Pub/Sub publish successful: channel={}", channel);
        } catch (Exception e) {
            // 推送出口失败不应影响主链路（Kafka -> DB/Redis）
            log.warn("Redis Pub/Sub publish failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
