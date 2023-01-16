package com.example.signalling2.repository;

import com.example.signalling2.domain.People;
import com.example.signalling2.domain.RoomSession;
import org.springframework.data.repository.CrudRepository;

public interface RoomRedisRepository extends CrudRepository<RoomSession, Long> {
    public RoomSession findById(String id);
}