package com.example.signalling2.config;

import org.kurento.client.KurentoClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KurentoConfig {
    @Bean
    public KurentoClient kurentoClient() {
        return KurentoClient.create();
        //return KurentoClient.create("ws://13.125.153.210:8888/kurento");
    }
}
