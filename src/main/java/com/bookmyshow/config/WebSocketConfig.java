package com.bookmyshow.config;

import com.bookmyshow.filter.ClerkJwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Spring WebSocket & STOMP Configuration for Live Seat Updates (Phase 5.1).
 * Configures STOMP endpoint at /ws/seats and simple in-memory broker at /topic.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ClerkJwtAuthenticationFilter clerkJwtAuthenticationFilter;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefix for STOMP destinations handled by @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
        // Enable simple in-memory message broker to carry messages back to clients on /topic
        registry.enableSimpleBroker("/topic", "/queue");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new org.springframework.messaging.support.ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authorization = accessor.getFirstNativeHeader("Authorization");
                    if (authorization == null || !authorization.startsWith("Bearer ")) {
                        throw new AccessDeniedException("Clerk authentication is required for live seat updates");
                    }
                    accessor.setUser(clerkJwtAuthenticationFilter.authenticate(authorization.substring(7).trim()));
                }
                return message;
            }
        });
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
