package com.example.kvm.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final VncProxyWebSocketHandler vncProxyWebSocketHandler;

    public WebSocketConfig(VncProxyWebSocketHandler vncProxyWebSocketHandler) {
        this.vncProxyWebSocketHandler = vncProxyWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(vncProxyWebSocketHandler, "/api/vnc-proxy/{vmName}")
                .setAllowedOrigins("*");
    }
}
