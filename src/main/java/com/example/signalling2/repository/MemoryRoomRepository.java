package com.example.signalling2.repository;

import com.example.signalling2.domain.Room;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class MemoryRoomRepository implements RoomRepository{
    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    @Override
    public Optional<Room> findById(String id) {
        return Optional.ofNullable(rooms.get(id));
    }


    @Override
    public Optional<Room> save(Room room) {
        rooms.put(room.getId(), room);
        return Optional.ofNullable(rooms.get(room.getId()));
    }

    @Override
    public void delete(String roomId) {
        rooms.remove(roomId);
    }

    @Override
    public List<String> findAll() {
        return  Collections.list(rooms.keys());
    }

}
