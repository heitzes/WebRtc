package com.example.signalling2.controller;

import com.example.signalling2.domain.UserSession;
import com.example.signalling2.dto.Response.ResponseDto;
import com.example.signalling2.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public UserSession getUser(@PathVariable String userId) {
        System.out.println("email received in user controller: " + userId);
        return userService.findById(userId);
    }

    @PostMapping
    public ResponseEntity<String> createUser(@RequestHeader(value="email") String email) {
        System.out.println(email);
        // test code
//        UserSession user = new UserSession(email);
        userService.createById(email);
        return ResponseDto.ok("user create");
    }
}
