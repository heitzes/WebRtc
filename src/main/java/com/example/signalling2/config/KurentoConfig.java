package com.example.signalling2.config;

import org.kurento.client.KurentoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KurentoConfig {
    @Value("${spring.kurento.host}")
    private String host;
    @Bean
    public KurentoClient kurentoClient() {
        return KurentoClient.create(host); // aws
    }
}
