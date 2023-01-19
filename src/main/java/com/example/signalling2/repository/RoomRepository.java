package com.example.signalling2.repository;

import com.example.signalling2.domain.Room;
import com.example.signalling2.domain.UserSession;

import java.util.List;
import java.util.Optional;

public interface RoomRepository {

    public Optional<Room> findById(String id);
    public Optional<Room> save(Room room);
    public void delete(String id);
    public List<String> findAll();

}
