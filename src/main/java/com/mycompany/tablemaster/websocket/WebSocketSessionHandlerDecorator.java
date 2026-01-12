package com.mycompany.tablemaster.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

/**
 * WebSocket 세션을 레지스트리에 등록/해제하는 Decorator
 */
@Slf4j
public class WebSocketSessionHandlerDecorator extends WebSocketHandlerDecorator {

    private final WebSocketSessionRegistry sessionRegistry;

    public WebSocketSessionHandlerDecorator(WebSocketHandler delegate, WebSocketSessionRegistry sessionRegistry) {
        super(delegate);
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessionRegistry.registerWebSocketSession(session.getId(), session);
        log.debug("WebSocket connection established: sessionId={}", session.getId());
        super.afterConnectionEstablished(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        sessionRegistry.removeWebSocketSession(session.getId());
        log.debug("WebSocket connection closed: sessionId={}, status={}", session.getId(), closeStatus);
        super.afterConnectionClosed(session, closeStatus);
    }
}
