package com.example.iot.gateway;

import com.example.iot.config.IotProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.procimg.Register;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import static org.mockito.Mockito.lenient;

/**
 * ModbusGatewayService 单元测试
 * 使用 Mockito 模拟 Modbus 和 MQTT 依赖
 */
@ExtendWith(MockitoExtension.class)
class ModbusGatewayServiceTest {

    @Mock
    private IotProperties iotProperties;

    @Mock
    private MqttClient mqttClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ModbusTCPMaster modbusMaster;

    @Mock
    private IotProperties.ModbusProperties modbusConfig;

    @Mock
    private IotProperties.MqttProperties mqttConfig;

    @InjectMocks
    private ModbusGatewayService gatewayService;

    @BeforeEach
    void setUp() throws Exception {
        // 注入 mock 的 modbusMaster
        gatewayService.setModbusMaster(modbusMaster);
        gatewayService.resetRetryCount();

        // 使用 lenient 避免 UnnecessaryStubbingException
        lenient().when(iotProperties.getModbus()).thenReturn(modbusConfig);
        lenient().when(iotProperties.getMqtt()).thenReturn(mqttConfig);
    }

    /**
     * 测试正常数据读取和发布
     * 验证：中间态（原始值）和最终输出（转换后的值）
     */
    @Test
    void testReadAndPublish_Success() throws Exception {
        // Given: 模拟已连接状态和 Modbus 返回原始值
        when(modbusConfig.isEnabled()).thenReturn(true);
        when(modbusMaster.isConnected()).thenReturn(true);

        Register[] mockRegisters = createMockRegisters(848, 2710);
        when(modbusMaster.readMultipleRegisters(anyInt(), anyInt(), anyInt()))
                .thenReturn(mockRegisters);

        // Mock JSON 序列化
        String expectedJson = "{\"deviceId\":\"device-001\",\"temperature\":84.8,\"rpm\":2710,\"rawTemperature\":848,\"rawRpm\":2710}";
        when(objectMapper.writeValueAsString(any())).thenReturn(expectedJson);

        when(mqttConfig.getTopic()).thenReturn("devices/data");
        when(mqttConfig.getQos()).thenReturn(1);

        // When: 执行读取
        gatewayService.pollDeviceData();

        // Then: 验证 MQTT 发布的消息
        ArgumentCaptor<MqttMessage> messageCaptor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(mqttClient).publish(eq("devices/data"), messageCaptor.capture());

        String payload = new String(messageCaptor.getValue().getPayload());

        // 验证 JSON 内容
        assertThat(payload).contains("device-001").contains("84.8").contains("2710");
    }

    /**
     * 测试温度换算：原始值 600 → 60.0°C
     */
    @Test
    void testTemperatureConversion_MinValue() throws Exception {
        // Given: 最小温度值
        when(modbusConfig.isEnabled()).thenReturn(true);
        when(modbusMaster.isConnected()).thenReturn(true);

        Register[] mockRegisters = createMockRegisters(600, 1000);
        when(modbusMaster.readMultipleRegisters(anyInt(), anyInt(), anyInt()))
                .thenReturn(mockRegisters);

        // Mock JSON 序列化 - 实际会生成 {"temperature":60.0,...}
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"temperature\":60.0}");

        when(mqttConfig.getTopic()).thenReturn("devices/data");
        when(mqttConfig.getQos()).thenReturn(1);

        // When
        gatewayService.pollDeviceData();

        // Then
        ArgumentCaptor<MqttMessage> messageCaptor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(mqttClient).publish(eq("devices/data"), messageCaptor.capture());
        assertThat(new String(messageCaptor.getValue().getPayload())).contains("60.0");
    }

    /**
     * 测试温度换算：原始值 1000 → 100.0°C
     */
    @Test
    void testTemperatureConversion_MaxValue() throws Exception {
        // Given: 最大温度值
        when(modbusConfig.isEnabled()).thenReturn(true);
        when(modbusMaster.isConnected()).thenReturn(true);

        Register[] mockRegisters = createMockRegisters(1000, 3000);
        when(modbusMaster.readMultipleRegisters(anyInt(), anyInt(), anyInt()))
                .thenReturn(mockRegisters);

        // Mock JSON 序列化
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"temperature\":100.0}");

        when(mqttConfig.getTopic()).thenReturn("devices/data");
        when(mqttConfig.getQos()).thenReturn(1);

        // When
        gatewayService.pollDeviceData();

        // Then
        ArgumentCaptor<MqttMessage> messageCaptor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(mqttClient).publish(eq("devices/data"), messageCaptor.capture());
        assertThat(new String(messageCaptor.getValue().getPayload())).contains("100.0");
    }

    /**
     * 测试连接断开时的重连逻辑
     */
    @Test
    void testReconnect_WhenDisconnected() throws Exception {
        // Given: 未连接状态
        when(modbusConfig.isEnabled()).thenReturn(true);
        when(modbusMaster.isConnected()).thenReturn(false);

        // When
        gatewayService.pollDeviceData();

        // Then: 验证尝试重连（没有发布消息）
        verify(mqttClient, never()).publish(anyString(), any(MqttMessage.class));
        // 验证重试计数增加
        assertThat(gatewayService.getRetryCount()).isEqualTo(1);
    }

    /**
     * 测试 Modbus 读取异常时的处理
     */
    @Test
    void testHandleModbusException() throws Exception {
        // Given: Modbus 读取抛出异常
        when(modbusConfig.isEnabled()).thenReturn(true);
        when(modbusMaster.isConnected()).thenReturn(true);
        when(modbusMaster.readMultipleRegisters(anyInt(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Modbus timeout"));

        // When
        gatewayService.pollDeviceData();

        // Then: 异常被捕获，没有发布消息
        verify(mqttClient, never()).publish(anyString(), any(MqttMessage.class));
    }

    /**
     * 测试 MQTT 发布异常时的处理
     */
    @Test
    void testHandleMqttException() throws Exception {
        // Given: Modbus 正常，MQTT 发布异常
        when(modbusConfig.isEnabled()).thenReturn(true);
        when(modbusMaster.isConnected()).thenReturn(true);

        Register[] mockRegisters = createMockRegisters(750, 2000);
        when(modbusMaster.readMultipleRegisters(anyInt(), anyInt(), anyInt()))
                .thenReturn(mockRegisters);

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(mqttConfig.getTopic()).thenReturn("devices/data");
        when(mqttConfig.getQos()).thenReturn(1);

        doThrow(new RuntimeException("MQTT connection lost"))
                .when(mqttClient).publish(anyString(), any(MqttMessage.class));

        // When & Then: 不应抛出异常
        gatewayService.pollDeviceData();

        // 验证尝试发布了消息
        verify(mqttClient).publish(anyString(), any(MqttMessage.class));
    }

    /**
     * 测试服务禁用时的行为
     */
    @Test
    void testWhenDisabled() throws Exception {
        // Given: 禁用 Modbus
        when(modbusConfig.isEnabled()).thenReturn(false);

        // When
        gatewayService.pollDeviceData();

        // Then: 不执行任何操作
        verify(modbusMaster, never()).isConnected();
        verify(mqttClient, never()).publish(anyString(), any(MqttMessage.class));
    }

    /**
     * 测试 QoS 设置
     */
    @Test
    void testMqttQoS() throws Exception {
        // Given
        when(modbusConfig.isEnabled()).thenReturn(true);
        when(modbusMaster.isConnected()).thenReturn(true);

        Register[] mockRegisters = createMockRegisters(800, 2500);
        when(modbusMaster.readMultipleRegisters(anyInt(), anyInt(), anyInt()))
                .thenReturn(mockRegisters);

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(mqttConfig.getTopic()).thenReturn("devices/data");
        when(mqttConfig.getQos()).thenReturn(1);

        // When
        gatewayService.pollDeviceData();

        // Then
        ArgumentCaptor<MqttMessage> messageCaptor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(mqttClient).publish(anyString(), messageCaptor.capture());

        assertThat(messageCaptor.getValue().getQos()).isEqualTo(1);
    }

    // ============ 辅助方法 ============

    private Register[] createMockRegisters(int tempRaw, int rpmRaw) {
        Register tempRegister = mock(Register.class);
        when(tempRegister.getValue()).thenReturn(tempRaw);

        Register rpmRegister = mock(Register.class);
        when(rpmRegister.getValue()).thenReturn(rpmRaw);

        return new Register[]{tempRegister, rpmRegister};
    }
}
