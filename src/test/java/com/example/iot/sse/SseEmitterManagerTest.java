package com.example.iot.infrastructure.sse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * SseEmitterManager 的单元测试。
 *
 * <p>覆盖连接注册、异常清理与心跳保活，确保线上 SSE 长连接在无业务数据时仍能被维护。</p>
 */
class SseEmitterManagerTest {

    /**
     * 验证 createEmitter 会注册 emitter，且 remove 可正确释放连接。
     */
    @Test
    @DisplayName("createEmitter 应注册 emitter，remove 应释放连接")
    void testCreateEmitterShouldRegisterAndRemove() {
        SseEmitterManager manager = new SseEmitterManager();

        SseEmitter emitter = manager.createEmitter("client-1");
        assertThat(emitter).isNotNull();
        assertThat(manager.hasClients()).isTrue();

        manager.remove("client-1");
        assertThat(manager.hasClients()).isFalse();
    }

    /**
     * 验证业务广播失败时会清理失效 emitter，避免坏连接长期滞留。
     */
    @Test
    @DisplayName("broadcast 发送失败时应移除失效 emitter")
    void testBroadcastShouldRemoveEmitterOnIOException() throws Exception {
        SseEmitterManager manager = new SseEmitterManager();
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new IOException("broken pipe")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        manager.register("client-2", emitter);
        assertThat(manager.hasClients()).isTrue();

        manager.broadcast("{\"deviceId\":\"device-001\"}");
        assertThat(manager.hasClients()).isFalse();
    }

    /**
     * 验证心跳会向在线 emitter 写出注释事件，降低反向代理因空闲而断流的概率。
     */
    @Test
    @DisplayName("sendHeartbeat 应向在线 emitter 发送心跳事件")
    void testSendHeartbeatShouldSendCommentEvent() throws Exception {
        SseEmitterManager manager = new SseEmitterManager();
        SseEmitter emitter = mock(SseEmitter.class);

        manager.register("client-3", emitter);
        manager.sendHeartbeat();

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        assertThat(manager.hasClients()).isTrue();
    }
}
