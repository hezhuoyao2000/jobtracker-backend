package com.example.iot.ingestion;

import com.example.iot.IotTestApplication;
import com.example.iot.config.IotProperties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 3 集成测试。
 *
 * <p>该测试类接入真实外部基础设施，验证完整链路：
 * MQTT 发布 -> MqttIngestionService 订阅并处理 -> Kafka 收到转发后的消息。
 * 因此它依赖本机可访问的 MQTT Broker 和 Kafka Broker。</p>
 */
@SpringBootTest(
        classes = IotTestApplication.class,
        properties = {
                "spring.main.web-application-type=none"
        }
)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MqttIngestionIntegrationTest {

    @Autowired
    private IotProperties iotProperties;

    private MqttClient publisherClient;
    private KafkaConsumer<String, String> kafkaConsumer;

    /**
     * 初始化真实 MQTT 发布端和真实 Kafka 消费端。
     *
     * <p>这里故意把 Kafka consumer 的 offset 策略设为 latest，
     * 避免把历史残留消息误判为本次测试结果。</p>
     */
    @BeforeAll
    void setUp() throws Exception {
        // 创建专用于测试发布的 MQTT 客户端，避免和业务 Consumer clientId 冲突。
        publisherClient = new MqttClient(
                iotProperties.getMqtt().getBroker(),
                "phase3-publisher-" + System.currentTimeMillis(),
                new MemoryPersistence()
        );

        MqttConnectOptions mqttOptions = new MqttConnectOptions();
        mqttOptions.setAutomaticReconnect(true);
        mqttOptions.setCleanSession(true);
        publisherClient.connect(mqttOptions);

        // 使用独立 group-id，避免和其他本地消费者互相影响。
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "phase3-it-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        kafkaConsumer = new KafkaConsumer<>(consumerProps);
        kafkaConsumer.subscribe(Collections.singletonList(iotProperties.getKafka().getTopic()));

        // 先 poll 一次以完成订阅和分区分配，减少首条消息丢失概率。
        kafkaConsumer.poll(Duration.ofSeconds(1));
    }

    /**
     * 关闭真实外部连接，避免测试结束后残留占用。
     */
    @AfterAll
    void tearDown() throws Exception {
        if (publisherClient != null && publisherClient.isConnected()) {
            publisherClient.disconnect();
            publisherClient.close();
        }
        if (kafkaConsumer != null) {
            kafkaConsumer.close();
        }
    }

    /**
     * 验证正常的 MQTT JSON 消息会被转发到 Kafka。
     */
    @Test
    @DisplayName("MQTT 消息应转发到 Kafka")
    void testMqttMessageForwardedToKafka() throws Exception {
        String payload = """
                {"deviceId":"device-phase3-it","temperature":88.6,"rpm":2200,"timestamp":"2026-04-12T12:30:00Z","rawTemperature":886,"rawRpm":2200}
                """.trim();

        // 发布真实 MQTT 消息，触发被测服务的 messageArrived 回调。
        publisherClient.publish(
                iotProperties.getMqtt().getConsumer().getTopic(),
                new MqttMessage(payload.getBytes())
        );

        // 从真实 Kafka topic 中轮询匹配记录。
        ConsumerRecord<String, String> record = pollForRecord("device-phase3-it", payload, Duration.ofSeconds(10));

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("device-phase3-it");
        assertThat(record.value()).isEqualTo(payload);
        assertThat(record.topic()).isEqualTo(iotProperties.getKafka().getTopic());
    }

    /**
     * 验证非法 JSON 不应进入 Kafka。
     *
     * <p>如果该测试失败，说明 ingestion 层在解析失败后仍然把脏数据转发出去了。</p>
     */
    @Test
    @DisplayName("非法 MQTT JSON 不应转发到 Kafka")
    void testInvalidJsonShouldNotBeForwardedToKafka() throws Exception {
        String invalidPayload = "{\"deviceId\":";

        publisherClient.publish(
                iotProperties.getMqtt().getConsumer().getTopic(),
                new MqttMessage(invalidPayload.getBytes())
        );

        ConsumerRecord<String, String> record = pollForRecord(null, invalidPayload, Duration.ofSeconds(5));

        assertThat(record).isNull();
    }

    /**
     * 在限定时间内从 Kafka 轮询目标记录。
     *
     * <p>该方法使用循环 poll，而不是固定 sleep，
     * 目的是既减少等待时间，又避免异步链路上的偶发抖动导致误报。</p>
     */
    private ConsumerRecord<String, String> pollForRecord(String expectedKey, String expectedValue, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();

        while (System.nanoTime() < deadline) {
            ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                // 非法 JSON 场景不关心 key，只关心 value 是否被错误转发。
                boolean keyMatches = expectedKey == null || expectedKey.equals(record.key());
                boolean valueMatches = expectedValue.equals(record.value());
                if (keyMatches && valueMatches) {
                    return record;
                }
            }
        }

        return null;
    }
}
