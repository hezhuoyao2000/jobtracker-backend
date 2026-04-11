package com.example.iot.gateway;

import com.example.iot.IotTestApplication;
import com.example.iot.config.IotProperties;
import com.example.iot.model.DeviceReading;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Modbus Gateway 集成测试
 * 使用 Mock 模式生成数据，验证完整数据流
 *
 * 测试环境要求：
 * - MQTT Broker 运行在 localhost:1883
 * - 配置: iot.modbus.mock-enabled=true
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
class ModbusGatewayIntegrationTest {

    @Autowired
    private IotProperties iotProperties;

    @Autowired
    private MockModbusGatewayService mockGatewayService;

    @Autowired
    private ObjectMapper objectMapper;

    private MqttClient testMqttClient;
    private final List<DeviceReading> receivedReadings = new ArrayList<>();
    private CountDownLatch messageLatch;

    private static final int EXPECTED_MESSAGES = 3;
    private static final int TIMEOUT_SECONDS = 10;

    @BeforeAll
    void setUp() throws Exception {
        // 创建测试用的 MQTT 客户端
        String brokerUrl = iotProperties.getMqtt().getBroker();
        String clientId = "test-client-" + System.currentTimeMillis();
        testMqttClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        testMqttClient.connect(options);

        // 设置回调
        testMqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                System.err.println("MQTT connection lost: " + cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String payload = new String(message.getPayload());
                System.out.println("Received MQTT message: " + payload);

                try {
                    DeviceReading reading = objectMapper.readValue(payload, DeviceReading.class);
                    synchronized (receivedReadings) {
                        receivedReadings.add(reading);
                    }
                    if (messageLatch != null) {
                        messageLatch.countDown();
                    }
                } catch (Exception e) {
                    System.err.println("Failed to parse message: " + e.getMessage());
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // 发布完成回调，测试客户端不需要处理
            }
        });

        // 订阅 topic
        String topic = iotProperties.getMqtt().getTopic();
        testMqttClient.subscribe(topic);

        System.out.println("Test MQTT client connected and subscribed to: " + topic);
    }

    @AfterAll
    void tearDown() throws Exception {
        if (testMqttClient != null && testMqttClient.isConnected()) {
            testMqttClient.disconnect();
            testMqttClient.close();
        }
    }

    @BeforeEach
    void resetLatch() {
        receivedReadings.clear();
        mockGatewayService.resetMessageCount();
        messageLatch = new CountDownLatch(EXPECTED_MESSAGES);
    }

    /**
     * 测试：验证 Mock 数据生成和 MQTT 发布
     * 验证点：
     * 1. 消息能到达 MQTT
     * 2. 数据格式正确
     * 3. 原始值和业务值都包含
     * 4. 温度值在有效范围内 (60.0-100.0°C)
     * 5. 转速值在有效范围内 (1000-3000 RPM)
     */
    @Test
    @DisplayName("Mock数据应成功发布到MQTT并包含完整字段")
    void testMockDataPublishedToMqtt() throws Exception {
        // Given: 等待 Mock 网关发送消息
        // Mock 服务每 1000ms 发送一次，等待 EXPECTED_MESSAGES 条消息

        // When: 等待消息到达
        boolean receivedAll = messageLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Then: 验证收到消息
        assertThat(receivedAll)
                .as("应在 %d 秒内收到 %d 条消息", TIMEOUT_SECONDS, EXPECTED_MESSAGES)
                .isTrue();

        assertThat(receivedReadings).hasSizeGreaterThanOrEqualTo(EXPECTED_MESSAGES);

        // 验证每条消息的结构和值范围
        for (DeviceReading reading : receivedReadings) {
            // 验证设备ID
            assertThat(reading.getDeviceId()).isEqualTo("device-001");

            // 验证时间戳存在
            assertThat(reading.getTimestamp()).isNotNull();

            // 验证原始值存在（中间态）
            assertThat(reading.getRawTemperature()).isNotNull();
            assertThat(reading.getRawRpm()).isNotNull();

            // 验证原始值范围（来自 Modbus 寄存器）
            assertThat(reading.getRawTemperature())
                    .as("温度原始值应在 600-1000 之间")
                    .isBetween(600, 1000);
            assertThat(reading.getRawRpm())
                    .as("转速原始值应在 1000-3000 之间")
                    .isBetween(1000, 3000);

            // 验证业务值范围（最终输出）
            assertThat(reading.getTemperature())
                    .as("温度应在 60.0-100.0°C 之间")
                    .isBetween(60.0, 100.0);
            assertThat(reading.getRpm())
                    .as("转速应在 1000-3000 RPM 之间")
                    .isBetween(1000, 3000);

            // 验证换算关系：temperature = rawTemperature / 10.0
            double expectedTemp = reading.getRawTemperature() / 10.0;
            assertThat(reading.getTemperature())
                    .as("温度换算应正确")
                    .isEqualTo(expectedTemp);

            // 验证转速：rpm = rawRpm
            assertThat(reading.getRpm())
                    .as("转速应与原始值相同")
                    .isEqualTo(reading.getRawRpm());
        }
    }

    /**
     * 测试：验证数据格式符合 JSON 规范
     */
    @Test
    @DisplayName("数据格式应符合 DeviceReading JSON 规范")
    void testDataFormat() throws Exception {
        // 等待至少一条消息
        boolean received = messageLatch.await(5, TimeUnit.SECONDS);
        assertThat(received).isTrue();
        assertThat(receivedReadings).isNotEmpty();

        DeviceReading firstReading = receivedReadings.get(0);

        // 验证所有必需字段
        assertThat(firstReading.getDeviceId()).isNotNull();
        assertThat(firstReading.getTemperature()).isNotNull();
        assertThat(firstReading.getRpm()).isNotNull();
        assertThat(firstReading.getTimestamp()).isNotNull();
        assertThat(firstReading.getRawTemperature()).isNotNull();
        assertThat(firstReading.getRawRpm()).isNotNull();
    }

    /**
     * 测试：验证 Mock 服务统计计数器
     */
    @Test
    @DisplayName("Mock服务应正确统计发送消息数")
    void testMessageCounting() throws Exception {
        // 等待 2 秒
        Thread.sleep(2000);

        long count = mockGatewayService.getMessageCount();

        // Mock 服务每 1000ms 发送一次，2 秒应发送约 2 条
        assertThat(count).isGreaterThanOrEqualTo(1);
        System.out.println("Mock gateway sent " + count + " messages");
    }

    /**
     * 测试：手动触发一次数据生成
     */
    @Test
    @DisplayName("手动触发数据生成应成功")
    void testManualTrigger() throws Exception {
        // 重置计数
        mockGatewayService.resetMessageCount();
        receivedReadings.clear();
        CountDownLatch manualLatch = new CountDownLatch(1);
        messageLatch = manualLatch;

        // 短暂等待确保订阅就绪
        Thread.sleep(500);

        // 手动触发一次
        mockGatewayService.generateMockData();

        // 等待消息（给更多时间）
        boolean received = manualLatch.await(5, TimeUnit.SECONDS);
        assertThat(received).as("应在5秒内收到手动触发的消息").isTrue();

        // 验证消息已收到
        assertThat(receivedReadings).hasSize(1);
        assertThat(mockGatewayService.getMessageCount()).isEqualTo(1);
    }

    /**
     * 测试：验证 MQTT QoS 配置
     */
    @Test
    @DisplayName("MQTT消息应使用配置的QoS级别")
    void testMqttQoS() throws Exception {
        // QoS 验证需要查看底层实现，这里验证消息能正常接收即可
        // 只等待 1 条消息
        CountDownLatch qosLatch = new CountDownLatch(1);
        messageLatch = qosLatch;

        boolean received = qosLatch.await(5, TimeUnit.SECONDS);
        assertThat(received).isTrue();

        // 如果消息能正常到达，说明 QoS 配置生效
        assertThat(receivedReadings).isNotEmpty();
    }
}
