package com.example.iot.controller;

import com.example.iot.sse.SseEmitterManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * IotSseController 的单元测试。
 *
 * <p>不启动 Web 容器，只验证 controller 会委托 SseEmitterManager 创建 emitter，
 * 并为线上 SSE 代理场景补齐必要响应头。</p>
 */
class IotSseControllerTest {

    /**
     * 验证 SSE 接口会创建 emitter，并显式关闭代理缓冲。
     */
    @Test
    @DisplayName("GET /api/iot/stream 应创建 emitter 并写入 SSE 响应头")
    void testStreamDeviceDataShouldCreateEmitter() {
        SseEmitterManager manager = mock(SseEmitterManager.class);
        SseEmitter emitter = new SseEmitter(0L);
        when(manager.createEmitter(anyString())).thenReturn(emitter);

        IotSseController controller = new IotSseController(manager);
        MockHttpServletResponse response = new MockHttpServletResponse();
        SseEmitter result = controller.streamDeviceData(response);

        assertThat(result).isSameAs(emitter);
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-cache");
        assertThat(response.getHeader("X-Accel-Buffering")).isEqualTo("no");
        assertThat(response.getHeader("Connection")).isEqualTo("keep-alive");
        verify(manager).createEmitter(anyString());
    }
}
