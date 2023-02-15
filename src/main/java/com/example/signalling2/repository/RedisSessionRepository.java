package com.example.signalling2.repository;

import com.example.signalling2.domain.RoomRedis;
import com.example.signalling2.domain.SessionRedis;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class RedisSessionRepository {
    private final RedisTemplate<String, SessionRedis> redisSessionTemplate;

    public Optional<SessionRedis> findById(String sessionId) {
        if (sessionId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((SessionRedis) redisSessionTemplate.opsForHash().get("session", sessionId));
    }

    public Optional<SessionRedis> save(String sessionId, SessionRedis sessionRedis) {
        redisSessionTemplate.opsForHash().put("session", sessionId, sessionRedis);
        return findById(sessionId);
    }

    public void delete(String sessionId) {
        redisSessionTemplate.opsForHash().delete("session", sessionId);
    }

    public Set<Object> findAll() {
        return redisSessionTemplate.opsForHash().keys("session");
    }
}
