package com.example.signalling2.repository;

import com.example.signalling2.domain.UserSession;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class MemoryUserRepository implements UserRepository {
    private final ConcurrentHashMap<String, UserSession> users = new ConcurrentHashMap<>();
    private long sequence = 0L;
    @Override
    public Optional<UserSession> findById(String id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<UserSession> save(UserSession userSession) {
        ++sequence;
        users.put(userSession.getId(), userSession);
        return Optional.ofNullable(users.get(userSession.getId()));
    }

    @Override
    public List<String> findAll() {
        return null;
    }

    @Override
    public long getSequence() {
        return sequence;
    }

    @Override
    public void delete(String id) {
        users.remove(id);
    }
}
