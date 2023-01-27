package com.example.signalling2.config;

import org.kurento.client.KurentoClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KurentoConfig {
    @Bean
    public KurentoClient kurentoClient() {
//        return KurentoClient.create(); //local
        return KurentoClient.create("ws://3.34.108.88:8888/kurento"); // aws
    }
}
