package com.example.signalling2.utils;

import com.example.signalling2.domain.UserSession;
import com.example.signalling2.exception.ServiceException;
import com.example.signalling2.service.UserService;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class SignalUtil {
    private final UserService userService;

    public void saveSession(final WebSocketSession session, String email) {
        try {
            userService.updateById(session, email);
        } catch (ServiceException e) {
            System.out.println("Catch ServiceException before decorator catch!");
            System.out.println("Can't save websocket session.");
            // fixme: 후속 조치
        }
    }
    public UserSession getUser(String email) {
        try {
            UserSession user = userService.findById(email);
            return user;
        } catch (ServiceException e) {
            System.out.println("Catch ServiceException before decorator catch!");
            System.out.println("Can't find user.");
            return null; // fixme: 유저 없으면 어칼건데?
        }
    }

    public WebRtcEndpoint getEndpoint(String email) {
        UserSession user = getUser(email);
        return user.getWebRtcEndpoint();
    }

    public static void sendMessage(WebSocketSession session, JsonObject response) {
        try {
            session.sendMessage(new TextMessage(response.toString()));
        } catch (Exception e) {
            // question: 웹소켓 연결이 끊어지면 이미 afterConnectionClosed 가 실행될것임
        }
    }
}
