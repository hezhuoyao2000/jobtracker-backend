package com.example.iot.ingestion;

import com.example.iot.config.IotProperties;
import com.example.iot.model.DeviceReading;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Phase 3 单元测试。
 *
 * <p>该测试类只验证 MqttIngestionService 自身的业务行为，不连接真实 MQTT/Kafka。
 * 外部依赖全部通过 Mockito 模拟，重点覆盖：
 * 1. 初始化订阅逻辑
 * 2. MQTT 消息到达后的 JSON 解析
 * 3. Kafka 转发参数是否正确
 * 4. 异常分支是否被安全吞掉
 * 5. 资源清理逻辑是否正确执行
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class MqttIngestionServiceTest {

    @Mock
    private IotProperties iotProperties;

    @Mock
    private MqttClient mqttConsumerClient;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private IotProperties.MqttProperties mqttProperties;

    @Mock
    private IotProperties.MqttProperties.ConsumerProperties consumerProperties;

    @Mock
    private IotProperties.KafkaProperties kafkaProperties;

    @InjectMocks
    private MqttIngestionService mqttIngestionService;

    /**
     * 为每个测试准备默认配置桩。
     *
     * <p>这里使用 lenient，是为了避免某些测试场景只覆盖其中一部分分支时触发
     * UnnecessaryStubbingException。测试只在关心的场景里覆写必要行为。</p>
     */
    @BeforeEach
    void setUp() {
        lenient().when(iotProperties.getMqtt()).thenReturn(mqttProperties);
        lenient().when(mqttProperties.getConsumer()).thenReturn(consumerProperties);
        lenient().when(iotProperties.getKafka()).thenReturn(kafkaProperties);
        lenient().when(consumerProperties.getTopic()).thenReturn("devices/data");
        lenient().when(consumerProperties.getQos()).thenReturn(1);
        lenient().when(kafkaProperties.getTopic()).thenReturn("device-data");
    }

    /**
     * 验证当 MQTT Consumer 开启时，服务会在初始化阶段完成：
     * 1. 回调注册
     * 2. topic 订阅
     */
    @Test
    void init_shouldSubscribeWhenConsumerEnabled() throws Exception {
        when(consumerProperties.isEnabled()).thenReturn(true);

        mqttIngestionService.init();

        verify(mqttConsumerClient).setCallback(mqttIngestionService);
        verify(mqttConsumerClient).subscribe("devices/data", 1);
    }

    /**
     * 验证当 MQTT Consumer 被关闭时，初始化阶段应直接返回，
     * 不应继续注册回调或订阅 topic。
     */
    @Test
    void init_shouldSkipWhenConsumerDisabled() throws Exception {
        when(consumerProperties.isEnabled()).thenReturn(false);

        mqttIngestionService.init();

        verify(mqttConsumerClient, never()).setCallback(any());
        verify(mqttConsumerClient, never()).subscribe(anyString(), anyInt());
    }

    /**
     * 验证正常消息链路：
     * MQTT payload -> 解析为 DeviceReading -> 发送到 Kafka。
     */
    @Test
    void messageArrived_shouldParsePayloadAndSendToKafka() throws Exception {
        // 使用确定性的 JSON 负载，避免断言依赖运行时动态值。
        var payload = """
                {"deviceId":"device-001","temperature":84.8,"rpm":2710,"timestamp":"2026-04-12T12:00:00Z","rawTemperature":848,"rawRpm":2710}
                """.trim();
        var reading = DeviceReading.builder()
                .deviceId("device-001")
                .temperature(84.8)
                .rpm(2710)
                .timestamp(Instant.parse("2026-04-12T12:00:00Z"))
                .rawTemperature(848)
                .rawRpm(2710)
                .build();

        // 模拟 JSON 解析成功，以及 Kafka 异步发送成功。
        when(objectMapper.readValue(payload, DeviceReading.class)).thenReturn(reading);
        when(kafkaTemplate.send("device-data", "device-001", payload))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        mqttIngestionService.messageArrived("devices/data", new MqttMessage(payload.getBytes()));

        verify(objectMapper).readValue(payload, DeviceReading.class);
        verify(kafkaTemplate).send("device-data", "device-001", payload);
    }

    /**
     * 验证非法 JSON 不会继续发送到 Kafka。
     *
     * <p>该场景要求服务只记录错误，不向上抛异常，避免影响 MQTT 消费线程。</p>
     */
    @Test
    void messageArrived_shouldIgnoreInvalidJson() throws Exception {
        var invalidPayload = "{invalid-json}";
        when(objectMapper.readValue(invalidPayload, DeviceReading.class))
                .thenThrow(new JsonProcessingException("bad json") {});

        assertThatCode(() ->
                mqttIngestionService.messageArrived("devices/data", new MqttMessage(invalidPayload.getBytes()))
        ).doesNotThrowAnyException();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    /**
     * 验证 Kafka 消息键使用 deviceId。
     *
     * <p>这关系到后续 Kafka 分区行为是否稳定，因此单独断言 topic、key 和 payload。</p>
     */
    @Test
    void messageArrived_shouldUseDeviceIdAsKafkaKey() throws Exception {
        var payload = """
                {"deviceId":"line-7","temperature":70.5,"rpm":1800,"timestamp":"2026-04-12T12:05:00Z"}
                """.trim();
        var reading = DeviceReading.builder()
                .deviceId("line-7")
                .temperature(70.5)
                .rpm(1800)
                .timestamp(Instant.parse("2026-04-12T12:05:00Z"))
                .build();

        when(objectMapper.readValue(payload, DeviceReading.class)).thenReturn(reading);
        when(kafkaTemplate.send(eq("device-data"), eq("line-7"), eq(payload)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        mqttIngestionService.messageArrived("devices/data", new MqttMessage(payload.getBytes()));

        // 捕获 KafkaTemplate.send 的实参，验证转发契约没有被破坏。
        var topicCaptor = ArgumentCaptor.forClass(String.class);
        var keyCaptor = ArgumentCaptor.forClass(String.class);
        var payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), payloadCaptor.capture());

        assertThatCode(() -> {
            org.assertj.core.api.Assertions.assertThat(topicCaptor.getValue()).isEqualTo("device-data");
            org.assertj.core.api.Assertions.assertThat(keyCaptor.getValue()).isEqualTo("line-7");
            org.assertj.core.api.Assertions.assertThat(payloadCaptor.getValue()).isEqualTo(payload);
        }).doesNotThrowAnyException();
    }

    /**
     * 验证关闭阶段会主动断开已建立的 MQTT 连接。
     */
    @Test
    void cleanup_shouldDisconnectConnectedClient() throws Exception {
        when(mqttConsumerClient.isConnected()).thenReturn(true);

        mqttIngestionService.cleanup();

        verify(mqttConsumerClient).disconnect();
    }

    /**
     * 验证即使断开连接时报错，清理阶段也不会继续向外抛异常。
     */
    @Test
    void cleanup_shouldSwallowDisconnectFailure() throws Exception {
        when(mqttConsumerClient.isConnected()).thenReturn(true);
        org.mockito.Mockito.doThrow(new MqttException(1)).when(mqttConsumerClient).disconnect();

        assertThatCode(() -> mqttIngestionService.cleanup()).doesNotThrowAnyException();
    }
}
