package com.example.iot.controller;

import com.example.iot.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * IoT SSE 推送端点。
 *
 * <p>约定：
 * - 前端发起 GET `/api/iot/stream`，浏览器会保持长连接。
 * - 只有连接存在时，服务端才会推送 Redis Pub/Sub 的设备数据。
 */
@RestController
@RequestMapping("/api/iot")
@RequiredArgsConstructor
public class IotSseController {

    private final SseEmitterManager sseEmitterManager;

    /**
     * 建立 SSE 连接并返回 emitter。
     *
     * <p>说明：
     * - produces 必须是 `text/event-stream`
     * - clientId 由服务端生成，用于管理连接生命周期
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDeviceData() {
        String clientId = UUID.randomUUID().toString();
        return sseEmitterManager.createEmitter(clientId);
    }
}

