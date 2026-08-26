package com.bookmyshow.config;

import com.bookmyshow.service.SeatLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Listener for WebSocket lifecycle events (connect, disconnect).
 * Ensures clean seat release when users close their browser or lose connection.
 */
@Slf4j
@Component
public class WebSocketEventListener {

    @Autowired
    private SeatLockService seatLockService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        log.debug("Received a new web socket connection: session [{}]", headerAccessor.getSessionId());
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.info("WebSocket session [{}] disconnected. Initiating seat lock cleanup.", sessionId);
        if (sessionId != null) {
            seatLockService.handleSessionDisconnect(sessionId);
        }
    }
}
