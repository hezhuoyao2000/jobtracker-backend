package com.example.iot.application;

import com.example.iot.config.IotProperties;
import com.example.iot.domain.DeviceReading;
import com.example.iot.infrastructure.sse.SseEmitterManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * 设备数据入库应用服务。
 *
 * <p>负责消费 Kafka payload 后的业务处理：写入 InfluxDB 历史数据、写入 Redis
 * 最新状态，并在存在 SSE 客户端时触发 Redis Pub/Sub 推送出口。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceDataIngestService {

    private static final String INFLUX_MEASUREMENT = "device_metrics";

    private final IotProperties iotProperties;
    private final Optional<InfluxDBClient> influxDBClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final SseEmitterManager sseEmitterManager;

    /**
     * 处理 Kafka 中的 DeviceReading JSON。
     *
     * @param payload Kafka 原始消息体
     */
    public void ingest(String payload) {
        try {
            DeviceReading reading = objectMapper.readValue(payload, DeviceReading.class);

            writeToInfluxDb(reading);
            String redisJson = writeToRedis(reading);
            publishToRedisPubSubIfNeeded(redisJson);

            log.info("Device data consumed successfully: deviceId={}, topic={}",
                    reading.getDeviceId(), iotProperties.getKafka().getTopic());
        } catch (Exception e) {
            log.error("Failed to consume Kafka message: {} - {}. Message: {}",
                    e.getClass().getSimpleName(), e.getMessage(), payload);
        }
    }

    /**
     * 将设备数据写入 InfluxDB 时间序列库。
     */
    void writeToInfluxDb(DeviceReading reading) {
        if (!iotProperties.getInfluxdb().isEnabled()) {
            return;
        }

        try {
            WriteApiBlocking writeApi = influxDBClient.orElseThrow().getWriteApiBlocking();
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
     */
    String writeToRedis(DeviceReading reading) {
        try {
            String key = iotProperties.getRedis().getKeyPrefix() + reading.getDeviceId();
            String value = objectMapper.writeValueAsString(reading);
            Duration ttl = Duration.ofSeconds(iotProperties.getRedis().getTtlSeconds());

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
     * 有 SSE 客户端在线时，把 Redis latest JSON 发布到 Pub/Sub channel。
     */
    void publishToRedisPubSubIfNeeded(String json) {
        if (json == null || json.isBlank()) {
            return;
        }
        if (!sseEmitterManager.hasClients()) {
            return;
        }

        try {
            String channel = iotProperties.getRedis().getPubsubChannel();
            stringRedisTemplate.convertAndSend(channel, json);
            log.debug("Redis Pub/Sub publish successful: channel={}", channel);
        } catch (Exception e) {
            log.warn("Redis Pub/Sub publish failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
