package com.bookmyshow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Spring WebSocket & STOMP Configuration for Live Seat Updates (Phase 5.1).
 * Configures STOMP endpoint at /ws/seats and simple in-memory broker at /topic.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefix for STOMP destinations handled by @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
        // Enable simple in-memory message broker to carry messages back to clients on /topic
        registry.enableSimpleBroker("/topic", "/queue");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint with SockJS fallback for browsers that don't support native WebSocket
        registry.addEndpoint("/ws/seats")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Native WebSocket endpoint for direct WebSocket clients and testing
        registry.addEndpoint("/ws/seats")
                .setAllowedOriginPatterns("*");
    }
}
