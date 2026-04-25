package com.example.iot.sse;

import com.example.iot.config.IotProperties;
import com.example.iot.infrastructure.sse.SseEmitterManager;
import com.example.myfirstspringboot.MyfirstspringbootApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Redis Pub/Sub -> SSE 转发的集成测试。
 *
 * <p>说明：
 * - 该测试依赖本地真实 Redis（spring.data.redis.*），用于验证 ListenerContainer + Subscriber 能正常工作。
 * - 不启动 Web 服务，不需要实际 SSE 客户端连接，只验证 subscriber 会调用 broadcast。
 */
@SpringBootTest(
        classes = MyfirstspringbootApplication.class,
        properties = {
                "spring.main.web-application-type=none",
                // 避免启动 Kafka listener，减少外部依赖带来的噪音
                "spring.kafka.listener.auto-startup=false",
                // 关闭不相关模块，减少测试日志噪音
                "iot.modbus.enabled=false",
                "iot.mqtt.consumer.enabled=false"
        }
)
class RedisPubSubIntegrationTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private IotProperties iotProperties;

    @SpyBean
    private SseEmitterManager sseEmitterManager;

    /**
     * 验证：向配置的 Pub/Sub channel 发布消息后，subscriber 能收到并调用 broadcast。
     */
    @Test
    @DisplayName("Redis Pub/Sub 消息应触发 SSE broadcast")
    void testRedisPubSubShouldTriggerBroadcast() {
        String channel = iotProperties.getRedis().getPubsubChannel();
        String json = "{\"deviceId\":\"device-001\",\"temperature\":1.0,\"rpm\":2}";

        stringRedisTemplate.convertAndSend(channel, json);

        // 使用 Mockito timeout 等待异步订阅回调触发
        verify(sseEmitterManager, timeout(2000)).broadcast(json);
    }
}
