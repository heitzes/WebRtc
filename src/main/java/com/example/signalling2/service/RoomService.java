package com.example.signalling2.service;
import com.example.signalling2.domain.Room;
import com.example.signalling2.domain.UserSession;
import com.example.signalling2.exception.ServiceException;
import com.example.signalling2.exception.errcode.ServiceErrorCode;
import com.example.signalling2.repository.MemoryRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final MemoryRoomRepository memoryRoomRepository;
    private long sequence = 0L;

    public Room createById(String roomId) {
        ++sequence;
        if (memoryRoomRepository.findById(roomId).isPresent()) {
            throw new ServiceException(ServiceErrorCode.ALREADY_IN);
        }
        Room room = new Room(roomId);
        return memoryRoomRepository.save(room).orElseThrow(()-> new ServiceException(ServiceErrorCode.NO_ROOM));
    }

    public Room findById(String roomId) {
        return memoryRoomRepository.findById(roomId).orElseThrow(()-> new ServiceException(ServiceErrorCode.NO_ROOM));
    }

    public List<String> findAll() {
        return memoryRoomRepository.findAll();
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

    public boolean isViewerExist(String roomId, String viewerId) {
        return findViewers(roomId).contains(viewerId); // fixme: room 존재 안하면 exception 생길텐데?
    }

    public void remove(String roomId) {
        --sequence;
        memoryRoomRepository.delete(roomId);
    }
}
