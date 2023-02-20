package com.example.signalling2.service;

import com.example.signalling2.domain.Room;
import com.example.signalling2.dto.Request.RoomCreateRequestDto;
import com.example.signalling2.dto.Response.RoomResponseDto;
import com.example.signalling2.exception.ServiceException;
import com.example.signalling2.exception.errcode.ServiceErrorCode;
import com.example.signalling2.repository.RoomRepository;
import com.example.signalling2.repository.ViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;
    private final ViewRepository viewRepository;

    public Room createById(RoomCreateRequestDto roomDTO) {
        Room room = new Room(roomDTO.getRoomId(), roomDTO.getTitle(), roomDTO.getProfileUrl());
        return roomRepository.save(room);
    }

    public void updateById(String pipeline, String roomId) {
        Room room = findById(roomId);
        room.setMediaPipeline(pipeline);
        roomRepository.save(room);
    }

    public RoomResponseDto createRoomResponseDto(String roomId, Room room) {
        String count = Integer.toString(viewRepository.findAll(roomId).size());
        return new RoomResponseDto(roomId, room.getTitle(), room.getProfileUrl(), count);
    }

    public Room findById(String roomId) {
        return roomRepository.findById(roomId).orElseThrow(()-> new ServiceException(ServiceErrorCode.NO_ROOM));
    }

    public ArrayList<RoomResponseDto> findAll() {
        ArrayList<RoomResponseDto> rooms = new ArrayList<>();
        for(Room room : roomRepository.findAll()) {
            RoomResponseDto roomResponseDto = createRoomResponseDto(room.getId(), room);
            rooms.add(roomResponseDto);
        }
        return rooms;
    }

    public void delete(String roomId) {
        roomRepository.deleteById(roomId);
        viewRepository.delete(roomId);
    }

//    public void releasePipeline(String roomId) {
//        Room room = findById(roomId);
//        if (room.getMediaPipeline() != null) {
//            mediaService.releaseMedia(room.getMediaPipeline(), room.getKurentoSessionId());
//        }
//    }

    public Set<String> findViewers(String roomId) {
        return viewRepository.findAll(roomId);
    }

    public void addViewer(String roomId, String viewerId) {
        viewRepository.save(roomId, viewerId);
    }

    public void subViewer(String roomId, String viewerId) {
        viewRepository.remove(roomId, viewerId);
    }

    public boolean isViewerExist(String roomId, String viewerId) {
        return viewRepository.find(roomId, viewerId);
    }
}
