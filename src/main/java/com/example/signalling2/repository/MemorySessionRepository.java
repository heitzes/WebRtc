package com.example.signalling2.repository;

import com.example.signalling2.domain.Session;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class MemorySessionRepository {
    private final static ConcurrentHashMap<String, Session> users = new ConcurrentHashMap<>();

    public Optional<Session> findById(String id) {
        return Optional.ofNullable(users.get(id));
    }

    public void save(Session session) {
        users.put(session.getId(), session);
    }

    public void delete(String id) {
        users.remove(id);
    }
}
