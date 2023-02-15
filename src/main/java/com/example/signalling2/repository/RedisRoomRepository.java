package com.example.signalling2.repository;

import com.example.signalling2.domain.RoomRedis;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class RedisRoomRepository {
    private final RedisTemplate<String, RoomRedis> redisRoomTemplate;

    public Optional<RoomRedis> findById(String roomId) {
        return Optional.ofNullable((RoomRedis) redisRoomTemplate.opsForHash().get("room", roomId));
    }

    public Optional<RoomRedis> save(String roomId, RoomRedis room) {
        redisRoomTemplate.opsForHash().put("room", roomId, room);
        return findById(roomId);
    }

    public void delete(String roomId) {
        redisRoomTemplate.opsForHash().delete("room", roomId);
    }

    public Set<Object> findAll() {
        return redisRoomTemplate.opsForHash().keys("room");
    }
}
