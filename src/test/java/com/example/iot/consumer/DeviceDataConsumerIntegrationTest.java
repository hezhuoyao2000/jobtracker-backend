package com.example.iot.consumer;

import com.example.iot.IotTestApplication;
import com.example.iot.config.IotProperties;
import com.example.iot.domain.DeviceReading;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 4 集成测试。
 *
 * <p>该测试接入真实 Kafka、Redis、InfluxDB，验证完整链路：
 * Kafka -> DeviceDataConsumer -> Redis + InfluxDB。</p>
 */
@SpringBootTest(
        classes = IotTestApplication.class,
        properties = {
                "spring.main.web-application-type=none",
                "iot.modbus.mock-enabled=false",
                "iot.influxdb.token=my-super-secret-token",
                "iot.influxdb.org=iot-demo",
                "iot.influxdb.bucket=device-metrics"
        }
)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DeviceDataConsumerIntegrationTest {

    @Autowired
    private IotProperties iotProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private InfluxDBClient influxDBClient;

    private KafkaProducer<String, String> kafkaProducer;

    @BeforeAll
    void setUp() {
        objectMapper.findAndRegisterModules();

        Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProperties.put(ProducerConfig.ACKS_CONFIG, "all");

        kafkaProducer = new KafkaProducer<>(producerProperties);
    }

    @AfterAll
    void tearDown() {
        if (kafkaProducer != null) {
            kafkaProducer.close();
        }
    }

    /**
     * 验证 Kafka 中的合法消息会被消费并同步写入 Redis 和 InfluxDB。
     */
    @Test
    @DisplayName("Kafka 消息应写入 Redis 和 InfluxDB")
    void testKafkaMessageShouldBePersistedToRedisAndInfluxDb() throws Exception {
        String deviceId = "device-phase4-it-" + UUID.randomUUID();
        String redisKey = iotProperties.getRedis().getKeyPrefix() + deviceId;

        DeviceReading reading = DeviceReading.builder()
                .deviceId(deviceId)
                .temperature(93.4)
                .rpm(2450)
                .timestamp(Instant.now())
                .rawTemperature(934)
                .rawRpm(2450)
                .build();

        String payload = objectMapper.writeValueAsString(reading);

        // 先清掉可能残留的 Redis 键，避免旧值干扰断言。
        stringRedisTemplate.delete(redisKey);

        kafkaProducer.send(new ProducerRecord<>(iotProperties.getKafka().getTopic(), deviceId, payload)).get();
        kafkaProducer.flush();

        String cachedValue = pollForRedisValue(redisKey, Duration.ofSeconds(10));
        assertThat(cachedValue).isEqualTo(payload);

        Long ttlSeconds = stringRedisTemplate.getExpire(redisKey);
        assertThat(ttlSeconds).isNotNull();
        assertThat(ttlSeconds).isPositive();
        assertThat(ttlSeconds).isLessThanOrEqualTo((long) iotProperties.getRedis().getTtlSeconds());

        List<FluxRecord> records = pollForInfluxRecords(deviceId, Duration.ofSeconds(10));
        assertThat(records).isNotEmpty();

        FluxRecord latestRecord = records.get(records.size() - 1);
        assertThat(latestRecord.getValueByKey("deviceId")).isEqualTo(deviceId);
        assertThat(((Number) latestRecord.getValueByKey("temperature")).doubleValue()).isEqualTo(93.4);
        assertThat(((Number) latestRecord.getValueByKey("rpm")).intValue()).isEqualTo(2450);
    }

    /**
     * 验证非法 JSON 不会写入 Redis 和 InfluxDB。
     */
    @Test
    @DisplayName("非法 Kafka JSON 不应写入 Redis 和 InfluxDB")
    void testInvalidKafkaMessageShouldNotBePersisted() throws Exception {
        String deviceId = "device-phase4-invalid-" + UUID.randomUUID();
        String invalidPayload = "{\"deviceId\":\"" + deviceId + "\"";
        String redisKey = iotProperties.getRedis().getKeyPrefix() + deviceId;

        stringRedisTemplate.delete(redisKey);

        kafkaProducer.send(new ProducerRecord<>(iotProperties.getKafka().getTopic(), deviceId, invalidPayload)).get();
        kafkaProducer.flush();

        String cachedValue = pollForRedisValue(redisKey, Duration.ofSeconds(5));
        assertThat(cachedValue).isNull();

        List<FluxRecord> records = pollForInfluxRecords(deviceId, Duration.ofSeconds(5));
        assertThat(records).isEmpty();
    }

    /**
     * 轮询 Redis，直到缓存值出现或超时。
     */
    private String pollForRedisValue(String redisKey, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();

        while (System.nanoTime() < deadline) {
            // Redis 是异步侧写，这里使用短轮询代替固定长时间 sleep。
            String value = stringRedisTemplate.opsForValue().get(redisKey);
            if (value != null) {
                return value;
            }
            Thread.sleep(250);
        }

        return null;
    }

    /**
     * 轮询 InfluxDB，直到目标设备数据出现或超时。
     */
    private List<FluxRecord> pollForInfluxRecords(String deviceId, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();

        while (System.nanoTime() < deadline) {
            List<FluxRecord> records = queryInfluxRecords(deviceId);
            if (!records.isEmpty()) {
                return records;
            }
            Thread.sleep(500);
        }

        return List.of();
    }

    /**
     * 查询指定设备最近 10 分钟内写入的 InfluxDB 记录。
     */
    private List<FluxRecord> queryInfluxRecords(String deviceId) {
        String bucket = iotProperties.getInfluxdb().getBucket();
        String flux = """
                from(bucket: "%s")
                  |> range(start: -10m)
                  |> filter(fn: (r) => r["_measurement"] == "device_metrics")
                  |> filter(fn: (r) => r["deviceId"] == "%s")
                  |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
                  |> sort(columns: ["_time"])
                """.formatted(bucket, deviceId);

        List<FluxTable> tables = influxDBClient.getQueryApi().query(flux, iotProperties.getInfluxdb().getOrg());
        return tables.stream()
                .flatMap(table -> table.getRecords().stream())
                .toList();
    }
}
