package com.example.iot.entrypoint.listener;

import com.example.iot.application.DeviceDataIngestService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka 设备数据监听入口。
 *
 * <p>该类集中声明 KafkaListener 注解，收到 device-data topic 消息后只负责
 * 委托应用服务处理。</p>
 */
@Component
@RequiredArgsConstructor
public class KafkaDeviceDataListener {

    private final DeviceDataIngestService deviceDataIngestService;

    /**
     * 接收 Kafka 设备数据消息。
     *
     * @param payload Kafka 中的 DeviceReading JSON
     */
    @KafkaListener(
            topics = "${iot.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id:iot-consumer-group}"
    )
    public void onMessage(String payload) {
        deviceDataIngestService.ingest(payload);
    }
}
