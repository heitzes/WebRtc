package com.example.signalling2.controller;

import com.example.signalling2.domain.UserSession;
import com.example.signalling2.service.RoomService;
import com.example.signalling2.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/{userId}")
    public UserSession getUser(@PathVariable String userId) {
        return userService.findById(userId);
    }
}
