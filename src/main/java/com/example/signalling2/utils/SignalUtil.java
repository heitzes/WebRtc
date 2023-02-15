package com.example.signalling2.utils;

import com.example.signalling2.domain.SessionRedis;
import com.example.signalling2.domain.UserSession;
import com.example.signalling2.exception.ServiceException;
import com.example.signalling2.service.RedisRoomService;
import com.example.signalling2.service.UserService;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class SignalUtil {
    private final UserService userService;
    private final RedisRoomService roomService;

    public void saveSession(final WebSocketSession session, String roomId, String email) {
        try {
            userService.updateById(session, email);
            userService.createSessionById(session.getId(), roomId, email);
        } catch (ServiceException e) {
            System.out.println("Catch ServiceException before decorator catch!");
            System.out.println("Can't save websocket session.");
            // fixme: 후속 조치
        }
    }

    public void deleteSession(String sessionId) {
        try {
            SessionRedis sessionRedis = userService.findSessionById(sessionId);
            String email = sessionRedis.getEmail(); // 웹소켓 끊어진 사람의 email
            String roomId = sessionRedis.getRoomId(); // 웹소켓 끊어진 사람이 속한 room
            if (email.equals(roomId)) { // notice: presenter이 끊김
                Set<String> viewers = roomService.findViewers(email); // 방에 속한 시청자들
                for (String viewerId : viewers) {
                    UserSession viewer = userService.findById(viewerId);
                    userService.leaveRoom(viewer.getSessionId(), viewerId);
                }
                userService.deleteRoom(sessionId, email);
                roomService.delete(email);
            } else { // notice: viewer가 끊김
                roomService.subViewer(roomId, sessionRedis.getEmail()); // 방에서 뷰어 없애주고
                userService.leaveRoom(sessionId, sessionRedis.getEmail());
            }
        } catch(ServiceException e) {
            System.out.println("WebSocket Session not exists.");
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
