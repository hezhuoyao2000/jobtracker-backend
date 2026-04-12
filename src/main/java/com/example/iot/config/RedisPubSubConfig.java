package com.example.iot.config;

import com.example.iot.sse.RedisDeviceDataSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis Pub/Sub 配置。
 *
 * <p>职责：
 * - 创建 RedisMessageListenerContainer
 * - 订阅 `iot.redis.pubsub-channel` 对应的 channel，把消息转发到 SSE
 */
@Configuration
@RequiredArgsConstructor
public class RedisPubSubConfig {

    private final IotProperties iotProperties;

    @Bean
    public ChannelTopic deviceDataChannelTopic() {
        return new ChannelTopic(iotProperties.getRedis().getPubsubChannel());
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisDeviceDataSubscriber redisDeviceDataSubscriber,
            ChannelTopic deviceDataChannelTopic
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 订阅 channel：DeviceDataConsumer 发布消息后，将由 RedisDeviceDataSubscriber 转发到 SSE
        container.addMessageListener(redisDeviceDataSubscriber, deviceDataChannelTopic);
        return container;
    }
}

