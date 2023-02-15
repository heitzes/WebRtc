package com.example.signalling2.service;

import com.example.signalling2.domain.RoomRedis;
import com.example.signalling2.dto.Request.RoomCreateRequestDto;
import com.example.signalling2.dto.Response.RoomResponseDto;
import com.example.signalling2.exception.ServiceException;
import com.example.signalling2.exception.errcode.ServiceErrorCode;
import com.example.signalling2.repository.RedisRoomRepository;
import com.example.signalling2.repository.RedisSessionRepository;
import com.example.signalling2.repository.RedisViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedisRoomService {
    private final RedisRoomRepository roomRepository;
    private final RedisViewRepository viewRepository;
    private final RedisSessionRepository sessionRepository;


    public RoomRedis createById(RoomCreateRequestDto roomDTO) {
        RoomRedis roomRedis = new RoomRedis(roomDTO.getTitle(), roomDTO.getProfileUrl());
        return roomRepository.save(roomDTO.getRoomId(), roomRedis).orElseThrow(()-> new ServiceException(ServiceErrorCode.NO_ROOM));
    }

    public RoomResponseDto createRoomResponseDto(String roomId, RoomRedis room) {
        String count = countViewers(roomId);
        return new RoomResponseDto(roomId, room.getTitle(), room.getProfileUrl(), count);
    }

    public RoomRedis findById(String roomId) {
        return roomRepository.findById(roomId).orElseThrow(()-> new ServiceException(ServiceErrorCode.NO_ROOM));
    }

    public ArrayList<RoomResponseDto> findAll() {
        ArrayList<RoomResponseDto> rooms = new ArrayList<>();
        //HashMap<String, RoomResponseDto> rooms = new HashMap<>();
        for (Object roomId : roomRepository.findAll()) {
            RoomResponseDto roomResponseDto = createRoomResponseDto(roomId.toString(), findById(roomId.toString()));
            rooms.add(roomResponseDto);
            //rooms.put(roomId.toString(), roomResponseDto);
        }
        return rooms;
    }

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

    public String countViewers(String roomId) {
        return Integer.toString(viewRepository.findAll(roomId).size());
    }

    public void delete(String roomId) {
        roomRepository.delete(roomId);
        viewRepository.delete(roomId);
    }
}
