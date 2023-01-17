package com.example.signalling2.repository;

import com.example.signalling2.domain.RoomSession;

import java.util.List;
import java.util.Optional;

public interface RoomRepository {
    public Optional<RoomSession> findById(String id);
    public void save(RoomSession roomSession);
    public List<String> findAll();
    public long getSequence();
    public void delete(String id);
}