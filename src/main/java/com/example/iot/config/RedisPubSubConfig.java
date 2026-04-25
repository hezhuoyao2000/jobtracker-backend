package com.example.iot.config;

import com.example.iot.entrypoint.listener.RedisDeviceDataListener;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis Pub/Sub 配置。
 *
 * <p>负责创建 RedisMessageListenerContainer，并把配置中的设备数据 channel
 * 绑定到 RedisDeviceDataListener 入口。</p>
 */
@Configuration
@RequiredArgsConstructor
public class RedisPubSubConfig {

    private final IotProperties iotProperties;

    /**
     * 创建设备数据 Pub/Sub channel。
     */
    @Bean
    public ChannelTopic deviceDataChannelTopic() {
        return new ChannelTopic(iotProperties.getRedis().getPubsubChannel());
    }

    /**
     * 注册 Redis 消息监听容器，让 Redis 推送入口能接收设备数据通知。
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisDeviceDataListener redisDeviceDataListener,
            ChannelTopic deviceDataChannelTopic
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(redisDeviceDataListener, deviceDataChannelTopic);
        return container;
    }
}
