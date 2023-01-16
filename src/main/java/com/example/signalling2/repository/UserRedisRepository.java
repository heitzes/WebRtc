package com.example.signalling2.repository;

import com.example.signalling2.domain.UserSession;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRedisRepository extends CrudRepository<UserSession, String> {
    public Optional<UserSession> findById(String id);
}