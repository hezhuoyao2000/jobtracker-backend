package com.example.iot.controller;

import com.example.iot.sse.SseEmitterManager;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * IoT SSE 长连接入口。
 *
 * <p>该控制器负责为前端创建 `text/event-stream` 响应，并补充代理友好的响应头，
 * 尽量降低线上反向代理对 SSE 的缓存、缓冲和空闲连接回收问题。</p>
 */
@RestController
@RequestMapping("/api/iot")
@RequiredArgsConstructor
public class IotSseController {

    private final SseEmitterManager sseEmitterManager;

    /**
     * 创建一个新的 SSE 连接，并显式关闭代理缓冲。
     *
     * @param response 当前 HTTP 响应，用于设置 SSE 相关响应头
     * @return 与当前客户端绑定的 SseEmitter
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDeviceData(HttpServletResponse response) {
        // 这些响应头用于提示反向代理不要缓存或缓冲 SSE 响应，降低线上 502/断流概率。
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Connection", "keep-alive");

        String clientId = UUID.randomUUID().toString();
        return sseEmitterManager.createEmitter(clientId);
    }
}
