package com.example.signalling2.controller;
import com.example.signalling2.domain.Room;
import com.example.signalling2.domain.UserSession;
import com.example.signalling2.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public List<String> getRooms() {
        return roomService.findAll();
    }

    @GetMapping("/{roomId}")
    public Room getRoom(@PathVariable String roomId) {
        return roomService.findById(roomId);
    }


}
