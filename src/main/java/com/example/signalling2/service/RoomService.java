package com.example.signalling2.service;
import com.example.signalling2.domain.Room;
import com.example.signalling2.domain.UserSession;
import com.example.signalling2.repository.MemoryRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final MemoryRoomRepository memoryRoomRepository;
    private long sequence = 0L;

    public Room save(Room room) {
        ++sequence;
        System.out.println("room sequence: " + sequence);
        return memoryRoomRepository.save(room).orElseThrow(()-> new RuntimeException("Room 저장 오류"));
    }

    public Room findById(String roomId) {
        return memoryRoomRepository.findById(roomId).orElseThrow(()-> new RuntimeException("Room 존재하지 않음"));
    }

    public List<String> findAll() {
        return memoryRoomRepository.findAll();
    }

    public UserSession findOwner(String roomId) {
        Room room = findById(roomId);
        return room.getOwner();
    }

    public ArrayList<String> findViewers(String roomId) {
        Room room = findById(roomId);
        return room.getViewers();
    }

    public void addViewer(String roomId, String viewerId) {
        Room room = findById(roomId);
        room.getViewers().add(viewerId);
        room.setViewCount(room.getViewCount()+1);
    }

    public void subViewer(String roomId, String viewerId) {
        Room room = findById(roomId);
        room.getViewers().remove(viewerId);
        room.setViewCount(room.getViewCount()-1);
    }

    public boolean isEmpty() {
        if (sequence == 0L) {
            return true;
        }
        return false;
    }

    public Boolean existById(String roomId) {
        return memoryRoomRepository.findById(roomId).isPresent();
    }


    public boolean isViewerExist(String roomId, String viewerId) {
        return findViewers(roomId).contains(viewerId); // fixme: room 존재 안하면 exception 생길텐데?
    }

    public void remove(String roomId) {
        --sequence;
        memoryRoomRepository.delete(roomId);
    }
}
