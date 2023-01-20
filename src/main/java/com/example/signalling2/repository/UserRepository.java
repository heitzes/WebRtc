package com.example.signalling2.repository;

import com.example.signalling2.domain.UserSession;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    public Optional<UserSession> findById(String id);
    public Optional<UserSession> save(UserSession userSession);
    public List<String> findAll();
    public long getSequence();
    public void delete(String id);
}
