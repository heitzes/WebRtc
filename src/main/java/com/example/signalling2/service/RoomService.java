package com.example.signalling2.service;

import com.example.signalling2.domain.Room;
import com.example.signalling2.domain.UserSession;
import com.example.signalling2.repository.MemoryRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final MemoryRoomRepository memoryRoomRepository;
    private long sequence = 0L;

    public Room save(Room room) {
        ++sequence;
        return memoryRoomRepository.save(room).orElseThrow(()-> new RuntimeException("Room 저장 오류"));
    }

    public Room findById(String roomId) {
        return memoryRoomRepository.findById(roomId).orElseThrow(()-> new RuntimeException("Room 존재하지 않음"));
    }

    public ArrayList<String> findViewers(String roomId) {
        Optional<Room> room = memoryRoomRepository.findById(roomId);
        ArrayList<String> viewers = room.get().getViewers();
        return viewers;
    }

    public void addViewer(String roomId, String viewerId) {
        Room room = this.findById(roomId);
        ArrayList<String> viewers = room.getViewers();
        viewers.add(viewerId);
        room.setViewCount(room.getViewCount()+1);
    }

    public void subViewer(String roomId, String viewerId) {
        Room room = this.findById(roomId);
        ArrayList<String> viewers = room.getViewers();
        viewers.remove(viewerId);
        room.setViewCount(room.getViewCount()-1);
    }

    public boolean isEmpty() {
        if (sequence == 0L) {
            return true;
        }
        return false;
    }

    public boolean isPresent(String roomId) {
        if (memoryRoomRepository.findById(roomId) == null) {
            return false;
        }
        return true;
    }

    public boolean isViewerExist(String roomId, String viewerId) {
        Optional<Room> room = memoryRoomRepository.findById(roomId);
        if (room.get()!=null) {
            if (room.get().getViewers().contains(viewerId)) {
                return true;
            }
        }
        return false;
    }

    public void remove(String roomId) {
        memoryRoomRepository.delete(roomId);
    }

}
