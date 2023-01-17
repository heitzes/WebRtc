package com.example.signalling2.controller;

import com.example.signalling2.repository.MemoryRoomRepository;
import com.example.signalling2.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class RoomController {

    private final MemoryRoomRepository roomRepository;

    @Autowired
    public RoomController(MemoryRoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @RequestMapping("/rooms")
    @ResponseBody // json type으로 반환
    public List<String> getRooms() {
        return roomRepository.findAll();
    }
}
