package com.example.signalling2.repository;

import com.example.signalling2.domain.RoomSession;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class MemoryRoomSessionRepository implements RoomSessionRepository {
    private final ConcurrentHashMap<String, RoomSession> rooms = new ConcurrentHashMap<>();
    private static long sequence = 0L;
    @Override
    public Optional<RoomSession> findById(String id) {
        return Optional.ofNullable(rooms.get(id));
    }

    @Override
    public void save(RoomSession roomSession) {
        ++sequence;
        rooms.put(roomSession.getId(), roomSession);
    }
    @Override
    public List<String> findAll() {
        return Collections.list(rooms.keys());
    }

    @Override
    public long getSequence() {
        return sequence;
    }

    @Override
    public void delete(String id) {
        rooms.remove(id);
    }


}
