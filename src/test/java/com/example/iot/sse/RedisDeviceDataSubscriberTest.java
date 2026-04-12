package com.example.iot.sse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * RedisDeviceDataSubscriber 的单元测试。
 *
 * <p>只验证“Redis 消息 -> SSE 广播”的转发行为，不依赖真实 Redis。</p>
 */
class RedisDeviceDataSubscriberTest {

    /**
     * 验证收到 Redis channel 消息后，会调用 SseEmitterManager.broadcast 进行转发。
     */
    @Test
    @DisplayName("收到 Redis 消息后应转发到 SSE")
    void testOnMessageShouldForwardToSse() {
        SseEmitterManager manager = mock(SseEmitterManager.class);
        RedisDeviceDataSubscriber subscriber = new RedisDeviceDataSubscriber(manager);

        String json = "{\"deviceId\":\"device-001\",\"temperature\":1.0,\"rpm\":2}";
        Message message = new DefaultMessage(
                "iot:device-data".getBytes(StandardCharsets.UTF_8),
                json.getBytes(StandardCharsets.UTF_8)
        );

        subscriber.onMessage(message, null);

        verify(manager).broadcast(json);
    }

    /**
     * 验证空消息不会触发广播，避免无意义调用。
     */
    @Test
    @DisplayName("空消息不应广播")
    void testOnMessageShouldIgnoreBlank() {
        SseEmitterManager manager = mock(SseEmitterManager.class);
        RedisDeviceDataSubscriber subscriber = new RedisDeviceDataSubscriber(manager);

        Message blank = new DefaultMessage(
                "iot:device-data".getBytes(StandardCharsets.UTF_8),
                "   ".getBytes(StandardCharsets.UTF_8)
        );

        subscriber.onMessage(blank, null);
        verify(manager, never()).broadcast(anyString());
    }
}
