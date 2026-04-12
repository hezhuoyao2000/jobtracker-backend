package com.example.iot.controller;

import com.example.iot.sse.SseEmitterManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * IotSseController 的轻量单元测试。
 *
 * <p>不启动 Web 容器，只验证 controller 会委托 SseEmitterManager 创建 emitter。</p>
 */
class IotSseControllerTest {

    /**
     * 验证调用 streamDeviceData 会返回 manager.createEmitter 生成的 emitter。
     */
    @Test
    @DisplayName("GET /api/iot/stream 应创建并返回 SseEmitter")
    void testStreamDeviceDataShouldCreateEmitter() {
        SseEmitterManager manager = mock(SseEmitterManager.class);
        SseEmitter emitter = new SseEmitter(0L);
        when(manager.createEmitter(anyString())).thenReturn(emitter);

        IotSseController controller = new IotSseController(manager);
        SseEmitter result = controller.streamDeviceData();

        assertThat(result).isSameAs(emitter);
        verify(manager).createEmitter(anyString());
    }
}

