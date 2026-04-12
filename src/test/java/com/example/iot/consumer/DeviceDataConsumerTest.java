package com.example.iot.consumer;

import com.example.iot.config.IotProperties;
import com.example.iot.model.DeviceReading;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Phase 4 单元测试。
 *
 * <p>该测试只验证 DeviceDataConsumer 的核心消费逻辑，
 * 下游依赖全部通过 Mockito mock 隔离，不连接真实 Kafka、Redis、InfluxDB。</p>
 */
@ExtendWith(MockitoExtension.class)
class DeviceDataConsumerTest {

    @Mock
    private InfluxDBClient influxDBClient;

    @Mock
    private WriteApiBlocking writeApiBlocking;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private IotProperties iotProperties;
    private DeviceDataConsumer deviceDataConsumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        // 构造最小可用配置，避免单元测试依赖 application.yml。
        iotProperties = new IotProperties();

        IotProperties.KafkaProperties kafkaProperties = new IotProperties.KafkaProperties();
        kafkaProperties.setTopic("device-data");
        iotProperties.setKafka(kafkaProperties);

        IotProperties.InfluxDbProperties influxDbProperties = new IotProperties.InfluxDbProperties();
        influxDbProperties.setBucket("test-bucket");
        iotProperties.setInfluxdb(influxDbProperties);

        IotProperties.RedisProperties redisProperties = new IotProperties.RedisProperties();
        redisProperties.setKeyPrefix("device:test:");
        redisProperties.setTtlSeconds(30);
        iotProperties.setRedis(redisProperties);

        // 使用 lenient 是为了兼容“非法 JSON”这类不会触发下游调用的测试场景。
        lenient().when(influxDBClient.getWriteApiBlocking()).thenReturn(writeApiBlocking);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        deviceDataConsumer = new DeviceDataConsumer(
                iotProperties,
                influxDBClient,
                stringRedisTemplate,
                objectMapper
        );
    }

    /**
     * 验证正常消息会同时写入 InfluxDB 和 Redis。
     */
    @Test
    @DisplayName("正常 Kafka 消息应写入 InfluxDB 和 Redis")
    void testOnMessageShouldWriteToInfluxDbAndRedis() throws Exception {
        DeviceReading reading = DeviceReading.builder()
                .deviceId("device-unit-001")
                .temperature(86.5)
                .rpm(2150)
                .timestamp(Instant.parse("2026-04-12T12:00:00Z"))
                .rawTemperature(865)
                .rawRpm(2150)
                .build();

        String payload = objectMapper.writeValueAsString(reading);

        deviceDataConsumer.onMessage(payload);

        // 捕获 InfluxDB 写入参数，确认消费链路已经触达时间序列存储。
        ArgumentCaptor<com.influxdb.client.write.Point> pointCaptor =
                ArgumentCaptor.forClass(com.influxdb.client.write.Point.class);
        verify(writeApiBlocking).writePoint(pointCaptor.capture());

        String lineProtocol = pointCaptor.getValue().toLineProtocol();
        assertThat(lineProtocol).contains("device_metrics");
        assertThat(lineProtocol).contains("deviceId=device-unit-001");
        assertThat(lineProtocol).contains("temperature=86.5");
        assertThat(lineProtocol).contains("rpm=2150");

        verify(valueOperations).set(
                eq("device:test:device-unit-001"),
                eq(payload),
                eq(Duration.ofSeconds(30))
        );
    }

    /**
     * 验证非法 JSON 不会触发任何下游写入。
     */
    @Test
    @DisplayName("非法 JSON 不应写入任何下游存储")
    void testOnMessageShouldIgnoreInvalidJson() {
        String invalidPayload = "{\"deviceId\":\"broken\"";

        deviceDataConsumer.onMessage(invalidPayload);

        verify(writeApiBlocking, never()).writePoint(any(com.influxdb.client.write.Point.class));
        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }

    /**
     * 验证缺少 timestamp 时仍可写入 InfluxDB。
     */
    @Test
    @DisplayName("缺少 timestamp 时应使用兜底时间继续写入")
    void testOnMessageShouldFallbackWhenTimestampMissing() throws Exception {
        DeviceReading reading = DeviceReading.builder()
                .deviceId("device-unit-no-ts")
                .temperature(72.3)
                .rpm(1800)
                .rawTemperature(723)
                .rawRpm(1800)
                .build();

        String payload = objectMapper.writeValueAsString(reading);

        deviceDataConsumer.onMessage(payload);

        verify(writeApiBlocking).writePoint(any(com.influxdb.client.write.Point.class));
        verify(valueOperations).set(
                eq("device:test:device-unit-no-ts"),
                eq(payload),
                eq(Duration.ofSeconds(30))
        );
    }

    /**
     * 验证 InfluxDB 写入异常会被吞掉，Redis 仍继续写入。
     */
    @Test
    @DisplayName("InfluxDB 写入失败时 Redis 仍应继续更新")
    void testOnMessageShouldContinueWhenInfluxDbWriteFails() throws Exception {
        DeviceReading reading = DeviceReading.builder()
                .deviceId("device-unit-influx-error")
                .temperature(91.2)
                .rpm(2600)
                .timestamp(Instant.parse("2026-04-12T13:00:00Z"))
                .rawTemperature(912)
                .rawRpm(2600)
                .build();

        String payload = objectMapper.writeValueAsString(reading);

        // 模拟时序库存储异常，验证当前消费链路会降级继续写 Redis。
        doThrow(new RuntimeException("influx write failed"))
                .when(writeApiBlocking)
                .writePoint(any(com.influxdb.client.write.Point.class));

        deviceDataConsumer.onMessage(payload);

        verify(writeApiBlocking).writePoint(any(com.influxdb.client.write.Point.class));
        verify(valueOperations).set(
                eq("device:test:device-unit-influx-error"),
                eq(payload),
                eq(Duration.ofSeconds(30))
        );
    }

    /**
     * 验证 Redis 写入异常不会反向影响 InfluxDB 写入。
     */
    @Test
    @DisplayName("Redis 写入失败时 InfluxDB 仍应成功写入")
    void testOnMessageShouldContinueWhenRedisWriteFails() throws Exception {
        DeviceReading reading = DeviceReading.builder()
                .deviceId("device-unit-redis-error")
                .temperature(68.1)
                .rpm(1500)
                .timestamp(Instant.parse("2026-04-12T14:00:00Z"))
                .rawTemperature(681)
                .rawRpm(1500)
                .build();

        String payload = objectMapper.writeValueAsString(reading);

        // 模拟缓存层瞬时异常，验证消息仍然可以落到 InfluxDB。
        doThrow(new RuntimeException("redis write failed"))
                .when(valueOperations)
                .set(any(), any(), any(Duration.class));

        deviceDataConsumer.onMessage(payload);

        verify(writeApiBlocking).writePoint(any(com.influxdb.client.write.Point.class));
        verify(valueOperations).set(
                eq("device:test:device-unit-redis-error"),
                eq(payload),
                eq(Duration.ofSeconds(30))
        );
    }
}
