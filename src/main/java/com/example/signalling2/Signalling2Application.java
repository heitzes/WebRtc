package com.example.signalling2;

import com.example.signalling2.repository.UserRedisRepository;
import com.example.signalling2.service.CallHandler;
import lombok.RequiredArgsConstructor;
import org.kurento.client.KurentoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@SpringBootApplication
@EnableWebSocket
@RequiredArgsConstructor
public class Signalling2Application implements WebSocketConfigurer {

    private final CallHandler callHandler;
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(callHandler, "/call")
                .setAllowedOrigins("*");
    }

    public static void main(String[] args) {
        SpringApplication.run(Signalling2Application.class, args);
    }

}
