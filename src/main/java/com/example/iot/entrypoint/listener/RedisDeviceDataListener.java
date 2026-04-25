package com.example.iot.entrypoint.listener;

import com.example.iot.infrastructure.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Redis Pub/Sub 设备数据监听入口。
 *
 * <p>该入口接收 Redis channel 中的设备 JSON，并转发给 SSE 管理器广播。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDeviceDataListener implements MessageListener {

    private final SseEmitterManager sseEmitterManager;

    /**
     * 收到 Redis Pub/Sub 消息后转发到所有 SSE 客户端。
     *
     * @param message Redis message，body 为 payload
     * @param pattern pattern 订阅参数，本项目使用 channel 订阅，通常为 null
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

        sseEmitterManager.broadcast(payload);
        log.debug("Redis Pub/Sub message forwarded to SSE: bytes={}", message.getBody().length);
    }
}
