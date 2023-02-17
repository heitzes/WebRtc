package com.example.signalling2.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
@RequiredArgsConstructor
public class ViewRepository {
    private final RedisTemplate<String, String> redisViewTemplate;

    public Boolean find(String roomId, String viewerId) {
        return redisViewTemplate.opsForSet().isMember(roomId, viewerId);
    }

    public void save(String roomId, String viewerId) {
        redisViewTemplate.opsForSet().add(roomId, viewerId);
    }

    public void remove(String roomId, String viewerId) {
        redisViewTemplate.opsForSet().remove(roomId, viewerId);
    }

    public void delete(String roomId) {
        redisViewTemplate.delete(roomId);
    }
    public Set<String> findAll(String roomId) {
        return redisViewTemplate.opsForSet().members(roomId);
    }
}
