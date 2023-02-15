package com.example.signalling2.config;

import com.example.signalling2.domain.RoomRedis;
import com.example.signalling2.domain.SessionRedis;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.HashSet;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;

    @Value("${spring.redis.database}")
    private int database;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // set config (host, port, password .. etc)
        var config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);
        config.setDatabase(database);   // 꼭 할당 받은 redis database 만 사용

        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, RoomRedis> redisRoomTemplate(RedisConnectionFactory rcf) {
        RedisTemplate<String, RoomRedis> redisRoomTemplate = new RedisTemplate<>();
        redisRoomTemplate.setConnectionFactory(rcf);
        redisRoomTemplate.setKeySerializer(new StringRedisSerializer());   // Key: String
        redisRoomTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(String.class));  // Value: 직렬화에 사용할 Object 사용하기
        return redisRoomTemplate;
    }

    @Bean
    public RedisTemplate<String, SessionRedis> redisSessionTemplate(RedisConnectionFactory rcf) {
        RedisTemplate<String, SessionRedis> redisSessionTemplate = new RedisTemplate<>();
        redisSessionTemplate.setConnectionFactory(rcf);
        redisSessionTemplate.setKeySerializer(new StringRedisSerializer());   // Key: String
        redisSessionTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(String.class));  // Value: 직렬화에 사용할 Object 사용하기
        return redisSessionTemplate;
    }

    @Bean
    public RedisTemplate<String, String> redisViewTemplate(RedisConnectionFactory rcf) {
        RedisTemplate<String, String> redisViewTemplate = new RedisTemplate<>();
        redisViewTemplate.setConnectionFactory(rcf);
        redisViewTemplate.setKeySerializer(new StringRedisSerializer());   // Key: String
        redisViewTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(String.class));  // Value: 직렬화에 사용할 Object 사용하기
        return redisViewTemplate;
    }
}