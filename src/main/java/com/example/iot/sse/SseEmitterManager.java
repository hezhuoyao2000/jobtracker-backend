package com.example.iot.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 连接管理器。
 *
 * <p>职责：
 * 1. 管理所有在线的 SSE 客户端连接（SseEmitter）。
 * 2. 提供广播能力：把一条消息推送给所有已连接客户端。
 *
 * <p>约束：
 * - 只有当客户端发起 GET `/api/iot/stream` 并保持连接时，才会收到推送数据。
 * - 没有任何客户端连接时，广播会快速返回，避免无意义开销。
 */
@Slf4j
@Component
public class SseEmitterManager {

    /**
     * clientId -> SseEmitter
     *
     * <p>使用 ConcurrentHashMap 保证并发安全：
     * - controller 线程注册 emitter
     * - redis listener / consumer 线程广播消息
     */
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 创建并注册一个 SSE emitter。
     *
     * @param clientId 客户端标识（由 controller 生成）
     * @return SseEmitter 实例（超时为 0，表示不超时）
     */
    public SseEmitter createEmitter(String clientId) {
        // 0L 表示不设置超时，由客户端断开触发清理
        SseEmitter emitter = new SseEmitter(0L);
        register(clientId, emitter);
        return emitter;
    }

    /**
     * 是否存在至少一个在线 SSE 客户端。
     *
     * <p>该方法用于决定是否需要发布 Redis Pub/Sub 消息：
     * - 无客户端时，DeviceDataConsumer 不发布，避免无意义刷新“推送出口”。
     */
    public boolean hasClients() {
        return !emitters.isEmpty();
    }

    /**
     * 广播一条 JSON 字符串给所有在线客户端。
     *
     * <p>注意：
     * - 这里的 payload 直接作为 data 发送给前端（EventSource.onmessage / addEventListener）。
     * - 发送失败会移除对应 emitter，避免持续失败占用资源。
     *
     * @param jsonPayload JSON 字符串（DeviceReading 的序列化结果）
     */
    public void broadcast(String jsonPayload) {
        if (emitters.isEmpty()) {
            return;
        }

        for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
            String clientId = entry.getKey();
            SseEmitter emitter = entry.getValue();
            try {
                emitter.send(SseEmitter.event()
                        .name("device-data")
                        .data(jsonPayload, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                // 发送失败通常表示客户端断开或网络异常，清理掉该连接
                log.debug("SSE send failed, remove emitter: clientId={}, msg={}", clientId, e.getMessage());
                remove(clientId);
            }
        }
    }

    /**
     * 注册一个 emitter，并绑定生命周期回调。
     *
     * <p>该方法对测试开放：单元测试可以注入 mock emitter，验证广播/清理逻辑。
     */
    void register(String clientId, SseEmitter emitter) {
        emitters.put(clientId, emitter);

        // 连接完成、超时或异常时，清理 emitter，避免内存泄漏
        emitter.onCompletion(() -> remove(clientId));
        emitter.onTimeout(() -> remove(clientId));
        emitter.onError(e -> remove(clientId));

        log.info("SSE emitter created: clientId={}", clientId);
    }

    /**
     * 移除 emitter。
     *
     * @param clientId 客户端标识
     */
    void remove(String clientId) {
        SseEmitter removed = emitters.remove(clientId);
        if (removed != null) {
            log.info("SSE emitter removed: clientId={}", clientId);
        }
    }
}

