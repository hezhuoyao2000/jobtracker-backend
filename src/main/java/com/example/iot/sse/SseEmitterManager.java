package com.example.iot.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 连接管理器。
 *
 * <p>负责维护所有在线 SseEmitter，并统一处理业务广播和心跳保活。
 * 心跳的目标不是传递业务数据，而是避免线上代理在消息空闲期主动回收长连接。</p>
 */
@Slf4j
@Component
public class SseEmitterManager {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 创建并注册一个永不超时的 SSE emitter。
     *
     * @param clientId 当前客户端唯一标识
     * @return 已注册的 SseEmitter
     */
    public SseEmitter createEmitter(String clientId) {
        SseEmitter emitter = new SseEmitter(0L);
        register(clientId, emitter);
        return emitter;
    }

    /**
     * 判断当前是否存在在线客户端，用于上游快速跳过无意义的 Pub/Sub 推送。
     */
    public boolean hasClients() {
        return !emitters.isEmpty();
    }

    /**
     * 将设备数据广播给所有在线 SSE 客户端。
     *
     * @param jsonPayload 设备数据 JSON
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
                log.debug("SSE send failed, remove emitter: clientId={}, msg={}", clientId, e.getMessage());
                remove(clientId);
            }
        }
    }

    /**
     * 定时发送心跳注释，避免线上代理把空闲 SSE 长连接当成死连接回收。
     */
    @Scheduled(fixedDelayString = "${iot.sse.heartbeat-interval-ms:15000}")
    public void sendHeartbeat() {
        if (emitters.isEmpty()) {
            return;
        }

        for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
            String clientId = entry.getKey();
            SseEmitter emitter = entry.getValue();
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException e) {
                log.debug("SSE heartbeat failed, remove emitter: clientId={}, msg={}", clientId, e.getMessage());
                remove(clientId);
            }
        }
    }

    /**
     * 注册 emitter 并绑定生命周期回调，确保异常连接会被及时清理。
     */
    void register(String clientId, SseEmitter emitter) {
        emitters.put(clientId, emitter);
        emitter.onCompletion(() -> remove(clientId));
        emitter.onTimeout(() -> remove(clientId));
        emitter.onError(e -> remove(clientId));
        log.info("SSE emitter created: clientId={}", clientId);
    }

    /**
     * 删除指定客户端对应的 emitter。
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
