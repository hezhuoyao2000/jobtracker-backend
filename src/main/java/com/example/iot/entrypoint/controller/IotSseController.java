package com.example.iot.entrypoint.controller;

import com.example.iot.infrastructure.sse.SseEmitterManager;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * IoT SSE HTTP 入口。
 *
 * <p>该控制器负责为前端创建 text/event-stream 响应，并关闭常见代理缓冲，
 * 实际连接管理交给 SseEmitterManager。</p>
 */
@RestController
@RequestMapping("/iot")
@RequiredArgsConstructor
public class IotSseController {

    private final SseEmitterManager sseEmitterManager;

    /**
     * 创建新的 SSE 连接。
     *
     * @param response 当前 HTTP 响应，用于设置 SSE 相关响应头
     * @return 与当前客户端绑定的 SseEmitter
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDeviceData(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Connection", "keep-alive");

        String clientId = UUID.randomUUID().toString();
        return sseEmitterManager.createEmitter(clientId);
    }
}
