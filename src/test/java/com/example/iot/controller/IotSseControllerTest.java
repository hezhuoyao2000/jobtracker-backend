package com.example.iot.controller;

import com.example.iot.entrypoint.controller.IotSseController;
import com.example.iot.infrastructure.sse.SseEmitterManager;
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
 * <p>该测试聚焦控制器方法本身，验证 SSE 响应头和 emitter 创建委托是否保持稳定。</p>
 */
class IotSseControllerTest {

    /**
     * 验证 /iot/stream 建连时会创建 emitter，并写入适合 SSE 长连接的响应头。
     */
    @Test
    @DisplayName("GET /iot/stream 应创建 emitter 并写入 SSE 响应头")
    void testStreamDeviceDataShouldCreateEmitter() {
        SseEmitterManager manager = mock(SseEmitterManager.class);
        SseEmitter emitter = new SseEmitter(0L);
        // 固定返回值，确保测试只验证控制器的委托和响应头行为。
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
