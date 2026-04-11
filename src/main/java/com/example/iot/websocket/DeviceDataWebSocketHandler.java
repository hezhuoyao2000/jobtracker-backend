package com.example.iot.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备数据 WebSocket Handler
 * 负责向连接的客户端推送实时数据
 */
@Slf4j
@Component
public class DeviceDataWebSocketHandler extends TextWebSocketHandler {

    // 存储所有活跃的 WebSocket Session
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: {}", session.getId());
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket connection closed: {}, status: {}", session.getId(), status);
        sessions.remove(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 处理客户端发来的消息（可选）
        log.debug("Received message from {}: {}", session.getId(), message.getPayload());
    }

    /**
     * 广播消息给所有连接的客户端
     */
    public void broadcast(String message) {
        if (sessions.isEmpty()) {
            return;
        }

        TextMessage textMessage = new TextMessage(message);
        sessions.forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (IOException e) {
                    log.error("Failed to send message to session {}: {}", session.getId(), e.getMessage());
                }
            }
        });
    }

    /**
     * 获取当前连接数
     */
    public int getConnectionCount() {
        return sessions.size();
    }
}
