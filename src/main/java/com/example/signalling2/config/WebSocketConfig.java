package com.example.signalling2.config;

import com.example.signalling2.handler.WebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.ExceptionWebSocketHandlerDecorator;


@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler webSocketHandler;

    @Bean
    public ExceptionWebSocketHandlerDecorator exceptionWebSocketHandlerDecorator() {
        // study: ExceptionWebSocketHandlerDecorator가 webSocketHandler에서 발생한 예외를 handleTransportError에게 전달해줌
        return new ExceptionWebSocketHandlerDecorator(webSocketHandler);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/call")
                .setAllowedOrigins("*");
    }
}
