package com.example.iot.entrypoint.scheduler;

import com.example.iot.infrastructure.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * SSE 心跳调度入口。
 *
 * <p>该任务定时触发 SSE 心跳，避免代理在无业务数据期间回收长连接。</p>
 */
@Component
@RequiredArgsConstructor
public class SseHeartbeatTask {

    private final SseEmitterManager sseEmitterManager;

    /**
     * 按配置周期发送 SSE 心跳。
     */
    @Scheduled(fixedDelayString = "${iot.sse.heartbeat-interval-ms:15000}")
    public void sendHeartbeat() {
        sseEmitterManager.sendHeartbeat();
    }
}
