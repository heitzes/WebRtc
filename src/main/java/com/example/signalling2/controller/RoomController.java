package com.example.signalling2.controller;

import com.example.signalling2.repository.MemoryRoomSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class RoomController {

    private final MemoryRoomSessionRepository roomRepository;

    @Autowired
    public RoomController(MemoryRoomSessionRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @RequestMapping("/rooms")
    @ResponseBody // json type으로 반환
    public List<String> getRooms() {
        return roomRepository.findAll();
    }
}
