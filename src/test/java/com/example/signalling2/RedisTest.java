package com.example.signalling2;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;


@ActiveProfiles("test")
@DisplayName("RedisTemplate basic 테스트")
@DataRedisTest
public class RedisTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @DisplayName("RedisTemplate hash CRUD 테스트")
    @Nested
    class RedisTemplateCrudTest {

        @Test
        void putAndGetHash() {
            // given
            final String key = "testkey3";
            final String hashKey = "testHashKey4";
            final String value = "testValue5";

            // when
            redisTemplate.opsForHash().put(key, hashKey, value);

            // then
            assertAll(
                    () -> assertEquals(redisTemplate.opsForHash().get(key, hashKey), value),
                    () -> assertEquals(redisTemplate.opsForHash().keys(key).size(), 1));
        }

        @Test
        void getStringTest() {
            // given
            final String key = "testkey";
            final String value = "hello";

            // when
            String redisValue = redisTemplate.opsForValue().get(key);

            // then
            assertEquals(value, redisValue);
        }
    }
}