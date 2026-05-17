package com.livescore.backend.Config;

import com.livescore.backend.WebSocketController.LiveChatHandler;
import com.livescore.backend.WebSocketController.LiveScoringHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;


@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LiveScoringHandler liveScoringHandler;
    private final LiveChatHandler liveChatHandler;
    public WebSocketConfig(LiveScoringHandler liveScoringHandler, LiveChatHandler liveChatHandler) {
        this.liveScoringHandler = liveScoringHandler;
        this.liveChatHandler = liveChatHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(liveScoringHandler, "/ws")
                .setAllowedOriginPatterns("*");
        registry.addHandler(liveChatHandler, "/ws/chat")
                .setAllowedOriginPatterns("*");

    }
}