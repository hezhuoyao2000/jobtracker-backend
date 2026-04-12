package com.example.iot.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Redis Pub/Sub 订阅器：把 Redis channel 的消息转发到 SSE。
 *
 * <p>数据来源：
 * - DeviceDataConsumer 在写入 Redis “latest” key 后，会把同一份 JSON 发布到一个 Pub/Sub channel。
 *
 * <p>约束：
 * - 这里不负责写入 InfluxDB/Redis，只负责“推送出口”。
 * - 当没有任何 SSE 客户端连接时，SseEmitterManager 会快速返回，相当于丢弃推送数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDeviceDataSubscriber implements MessageListener {

    private final SseEmitterManager sseEmitterManager;

    /**
     * 收到 Redis 消息后，转发到所有 SSE 客户端。
     *
     * @param message Redis message（body 为 payload）
     * @param pattern pattern（本项目使用 channel 订阅，该值通常为 null）
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        if (message == null || message.getBody() == null || message.getBody().length == 0) {
            return;
        }

        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        if (payload.isBlank()) {
            return;
        }

        // 将 Redis 推送的 JSON 原样转发给前端（前端按 JSON 解析即可）
        sseEmitterManager.broadcast(payload);
        log.debug("Redis Pub/Sub message forwarded to SSE: bytes={}", message.getBody().length);
    }
}

