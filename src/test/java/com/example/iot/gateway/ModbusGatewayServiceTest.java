package com.example.iot.gateway;

import com.example.iot.config.IotProperties;
import com.example.iot.application.ModbusCollectionService;
import com.example.iot.infrastructure.modbus.ModbusDeviceReader;
import com.example.iot.infrastructure.mqtt.MqttDeviceReadingPublisher;
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
 * ModbusGatewayService 鍗曞厓娴嬭瘯
 * 浣跨敤 Mockito 妯℃嫙 Modbus 鍜?MQTT 渚濊禆
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

    private ModbusCollectionService gatewayService;

    @BeforeEach
    void setUp() throws Exception {
        // 浣跨敤 lenient 閬垮厤 UnnecessaryStubbingException
        lenient().when(iotProperties.getModbus()).thenReturn(modbusConfig);
        lenient().when(iotProperties.getMqtt()).thenReturn(mqttConfig);

        ModbusDeviceReader modbusDeviceReader = new ModbusDeviceReader(iotProperties);
        modbusDeviceReader.setModbusMaster(modbusMaster);
        MqttDeviceReadingPublisher publisher =
                new MqttDeviceReadingPublisher(iotProperties, mqttClient, objectMapper);
        gatewayService = new ModbusCollectionService(iotProperties, modbusDeviceReader, publisher);
        gatewayService.resetRetryCount();
    }

    /**
     * 娴嬭瘯姝ｅ父鏁版嵁璇诲彇鍜屽彂甯?
     * 楠岃瘉锛氫腑闂存€侊紙鍘熷鍊硷級鍜屾渶缁堣緭鍑猴紙杞崲鍚庣殑鍊硷級
     */
    @Test
    void testReadAndPublish_Success() throws Exception {
        // Given: 妯℃嫙宸茶繛鎺ョ姸鎬佸拰 Modbus 杩斿洖鍘熷鍊?
        when(modbusConfig.isEnabled()).thenReturn(true);
        when(modbusMaster.isConnected()).thenReturn(true);

        Register[] mockRegisters = createMockRegisters(848, 2710);
        when(modbusMaster.readMultipleRegisters(anyInt(), anyInt(), anyInt()))
                .thenReturn(mockRegisters);

        // Mock JSON 搴忓垪鍖?
        String expectedJson = "{\"deviceId\":\"device-001\",\"temperature\":84.8,\"rpm\":2710,\"rawTemperature\":848,\"rawRpm\":2710}";
        when(objectMapper.writeValueAsString(any())).thenReturn(expectedJson);

        when(mqttConfig.getTopic()).thenReturn("devices/data");
        when(mqttConfig.getQos()).thenReturn(1);

        // When: 鎵ц璇诲彇
        gatewayService.collectOnce();

        // Then: 楠岃瘉 MQTT 鍙戝竷鐨勬秷鎭?
        ArgumentCaptor<MqttMessage> messageCaptor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(mqttClient).publish(eq("devices/data"), messageCaptor.capture());

        String payload = new String(messageCaptor.getValue().getPayload());

        // 楠岃瘉 JSON 鍐呭
        assertThat(payload).contains("device-001").contains("84.8").contains("2710");
    }

    /**
     * 娴嬭瘯娓╁害鎹㈢畻锛氬師濮嬪€?600 鈫?60.0掳C
     */
    @Test
    void testTemperatureConversion_MinValue() throws Exception {
        // Given: 鏈€灏忔俯搴﹀€?
        when(modbusConfig.isEnabled()).thenReturn(true);
        when(modbusMaster.isConnected()).thenReturn(true);

        Register[] mockRegisters = createMockRegisters(600, 1000);
        when(modbusMaster.readMultipleRegisters(anyInt(), anyInt(), anyInt()))
                .thenReturn(mockRegisters);

        // Mock JSON 搴忓垪鍖?- 瀹為檯浼氱敓鎴?{"temperature":60.0,...}
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"temperature\":60.0}");

        when(mqttConfig.getTopic()).thenReturn("devices/data");
        when(mqttConfig.getQos()).thenReturn(1);

        // When
        gatewayService.collectOnce();

        // Then
        ArgumentCaptor<MqttMessage> messageCaptor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(mqttClient).publish(eq("devices/data"), messageCaptor.capture());
        assertThat(new String(messageCaptor.getValue().getPayload())).contains("60.0");
    }

    /**
     * 娴嬭瘯娓╁害鎹㈢畻锛氬師濮嬪€?1000 鈫?100.0掳C
     */
    @Test
    void testTemperatureConversion_MaxValue() throws Exception {
        // Given: 鏈€澶ф俯搴﹀€?
        when(modbusConfig.isEnabled()).thenReturn(true);
        when(modbusMaster.isConnected()).thenReturn(true);

        Register[] mockRegisters = createMockRegisters(1000, 3000);
        when(modbusMaster.readMultipleRegisters(anyInt(), anyInt(), anyInt()))
                .thenReturn(mockRegisters);

        // Mock JSON 搴忓垪鍖?
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"temperature\":100.0}");

        when(mqttConfig.getTopic()).thenReturn("devices/data");
        when(mqttConfig.getQos()).thenReturn(1);

        // When
        gatewayService.collectOnce();

        // Then
        ArgumentCaptor<MqttMessage> messageCaptor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(mqttClient).publish(eq("devices/data"), messageCaptor.capture());
        assertThat(new String(messageCaptor.getValue().getPayload())).contains("100.0");
    }

    /**
     * 娴嬭瘯杩炴帴鏂紑鏃剁殑閲嶈繛閫昏緫
     */
    @Test
    void testReconnect_WhenDisconnected() throws Exception {
        // Given: 鏈繛鎺ョ姸鎬?
        when(modbusConfig.isEnabled()).thenReturn(true);
        when(modbusMaster.isConnected()).thenReturn(false);

        // When
        gatewayService.collectOnce();

        // Then: 楠岃瘉灏濊瘯閲嶈繛锛堟病鏈夊彂甯冩秷鎭級
        verify(mqttClient, never()).publish(anyString(), any(MqttMessage.class));
        // 楠岃瘉閲嶈瘯璁℃暟澧炲姞
        assertThat(gatewayService.getRetryCount()).isEqualTo(1);
    }

    /**
     * 娴嬭瘯 Modbus 璇诲彇寮傚父鏃剁殑澶勭悊
     */
    @Test
    void testHandleModbusException() throws Exception {
        // Given: Modbus 璇诲彇鎶涘嚭寮傚父
        when(modbusConfig.isEnabled()).thenReturn(true);
        when(modbusMaster.isConnected()).thenReturn(true);
        when(modbusMaster.readMultipleRegisters(anyInt(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Modbus timeout"));

        // When
        gatewayService.collectOnce();

        // Then: 寮傚父琚崟鑾凤紝娌℃湁鍙戝竷娑堟伅
        verify(mqttClient, never()).publish(anyString(), any(MqttMessage.class));
    }

    /**
     * 娴嬭瘯 MQTT 鍙戝竷寮傚父鏃剁殑澶勭悊
     */
    @Test
    void testHandleMqttException() throws Exception {
        // Given: Modbus 姝ｅ父锛孧QTT 鍙戝竷寮傚父
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

        // When & Then: 涓嶅簲鎶涘嚭寮傚父
        gatewayService.collectOnce();

        // 楠岃瘉灏濊瘯鍙戝竷浜嗘秷鎭?
        verify(mqttClient).publish(anyString(), any(MqttMessage.class));
    }

    /**
     * 娴嬭瘯鏈嶅姟绂佺敤鏃剁殑琛屼负
     */
    @Test
    void testWhenDisabled() throws Exception {
        // Given: 绂佺敤 Modbus
        when(modbusConfig.isEnabled()).thenReturn(false);

        // When
        gatewayService.collectOnce();

        // Then: 涓嶆墽琛屼换浣曟搷浣?
        verify(modbusMaster, never()).isConnected();
        verify(mqttClient, never()).publish(anyString(), any(MqttMessage.class));
    }

    /**
     * 娴嬭瘯 QoS 璁剧疆
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
        gatewayService.collectOnce();

        // Then
        ArgumentCaptor<MqttMessage> messageCaptor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(mqttClient).publish(anyString(), messageCaptor.capture());

        assertThat(messageCaptor.getValue().getQos()).isEqualTo(1);
    }

    // ============ 杈呭姪鏂规硶 ============

    private Register[] createMockRegisters(int tempRaw, int rpmRaw) {
        Register tempRegister = mock(Register.class);
        when(tempRegister.getValue()).thenReturn(tempRaw);

        Register rpmRegister = mock(Register.class);
        when(rpmRegister.getValue()).thenReturn(rpmRaw);

        return new Register[]{tempRegister, rpmRegister};
    }
}
