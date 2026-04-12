package com.example.iot.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka 主题配置。
 *
 * <p>这里统一维护 IoT 主题的 broker 侧属性，避免把 Topic 生命周期误配到
 * consumer/producer 客户端属性中导致配置不生效。</p>
 */
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private final IotProperties iotProperties;

    /**
     * 创建设备数据 Topic，并写入消息保留时长。
     *
     * <p>仅当 broker 可连接时，Spring Kafka Admin 才能完成该配置下发。</p>
     *
     * @return 设备数据 Topic 定义
     */
    @Bean
    public NewTopic deviceDataTopic() {
        return TopicBuilder.name(iotProperties.getKafka().getTopic())
                .partitions(1)
                .replicas(1)
                .config(
                        TopicConfig.RETENTION_MS_CONFIG,
                        String.valueOf(iotProperties.getKafka().getRetentionMs())
                )
                .build();
    }
}
