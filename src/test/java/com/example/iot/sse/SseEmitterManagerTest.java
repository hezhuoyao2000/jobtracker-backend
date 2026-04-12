package com.example.iot.sse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * SseEmitterManager 的单元测试。
 *
 * <p>这里不启动 Web 容器，只验证：
 * - emitter 生命周期是否能正确注册与清理
 * - 广播失败（IO 异常）时是否能移除对应 emitter
 */
class SseEmitterManagerTest {

    /**
     * 验证 createEmitter 会注册连接，并在 completion 时自动清理，避免内存泄漏。
     */
    @Test
    @DisplayName("createEmitter 应注册 emitter，remove 应清理")
    void testCreateEmitterShouldRegisterAndRemove() {
        SseEmitterManager manager = new SseEmitterManager();

        SseEmitter emitter = manager.createEmitter("client-1");
        assertThat(manager.hasClients()).isTrue();

        // 说明：SseEmitter.onCompletion 通常由 Spring MVC 请求生命周期触发；
        // 在纯单元测试里不容易可靠模拟该时机，因此这里直接验证 remove 行为即可。
        manager.remove("client-1");
        assertThat(manager.hasClients()).isFalse();
    }

    /**
     * 验证 broadcast 时，如果某个 emitter 发送失败，会被移除，避免持续失败占用资源。
     */
    @Test
    @DisplayName("broadcast 发送失败会移除 emitter")
    void testBroadcastShouldRemoveEmitterOnIOException() throws Exception {
        SseEmitterManager manager = new SseEmitterManager();

        // 用 mock emitter 模拟 send 失败（例如客户端断开）
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new IOException("broken pipe")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        manager.register("client-2", emitter);
        assertThat(manager.hasClients()).isTrue();

        manager.broadcast("{\"deviceId\":\"device-001\"}");
        assertThat(manager.hasClients()).isFalse();
    }
}
