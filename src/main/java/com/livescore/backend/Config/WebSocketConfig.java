package com.livescore.backend.Config;

import com.livescore.backend.WebSocketController.LiveScoringHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;


@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LiveScoringHandler liveScoringHandler;

    public WebSocketConfig(LiveScoringHandler liveScoringHandler) {
        this.liveScoringHandler = liveScoringHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(liveScoringHandler, "/ws")
                .setAllowedOriginPatterns("*");
    }
}