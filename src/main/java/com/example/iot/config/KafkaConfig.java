package com.example.iot.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka 配置
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /**
     * 创建设备数据 Topic（如果不存在）
     */
    @Bean
    public NewTopic deviceDataTopic() {
        return TopicBuilder.name("device-data")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
