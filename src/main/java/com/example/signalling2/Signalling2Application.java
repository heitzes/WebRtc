package com.example.signalling2;

import com.example.signalling2.service.CallHandler;
import org.kurento.client.KurentoClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@SpringBootApplication
@EnableWebSocket
public class Signalling2Application implements WebSocketConfigurer {
    @Bean
    public CallHandler callHandler() {
        return new CallHandler();
    }


    @Bean
    public KurentoClient kurentoClient() {
        return KurentoClient.create();
    }


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(callHandler(), "/call")
                .setAllowedOrigins("*");
    }

    public static void main(String[] args) {
        SpringApplication.run(Signalling2Application.class, args);
    }

}
