// src/main/java/com/project/ome/marketdata/config/WebSocketConfig.java
package com.project.ome.marketdata.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // tighten in production
                .withSockJS();                  // fallback for browsers

        // Also register without SockJS for native WS clients (Postman)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Clients subscribe to topics under /topic (public)
        // and /user/queue (private, per-user)
        registry.enableSimpleBroker("/topic", "/user");

        // Client sends messages to /app/... (not used much in our case)
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix for user-specific messages
        registry.setUserDestinationPrefix("/user");
    }
}