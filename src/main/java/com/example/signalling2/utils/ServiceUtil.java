package com.example.signalling2.utils;

import com.example.signalling2.domain.Room;
import com.example.signalling2.domain.User;
import com.example.signalling2.domain.Session;
import com.example.signalling2.exception.ServiceException;
import com.example.signalling2.service.MediaService;
import com.example.signalling2.service.RoomService;
import com.example.signalling2.service.SessionService;
import com.example.signalling2.service.UserService;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;

/**
 * WebSocketHandler 에서
 * 직접 service 관련 로직을 실행한다면, 실행시 발생할 수 있는 exception을
 * WebSocketHandler에서 처리하지 않으면 자동으로 ExceptionWebSocketHandlerDecorator 클래스로 전파되기 때문에
 * 실행해야할 service 관련 로직은 이 클래스를 통해 실행하고,
 * 이 클래스에서 exception을 catch합니다.
 *
 * WebSocketHandler에서도 try-catch로 exception 핸들링이 가능하지만,
 * 코드 가독성이 떨어지게 되고,
 * WebSocketHandler의 주 목적은 시그널 서버를 사용하는 이유인
 * sdp Negotiation, Ice candidate gathering 이기 때문에
 * 이 클래스로 분리했습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceUtil {
    private final RoomService roomService;
    private final UserService userService;
    private final MediaService mediaService;
    private final SessionService sessionService;

    public WebRtcEndpoint getEndpoint(String email) {
        try {
            User user = userService.findById(email);
            return mediaService.getEndpoint(user.getWebRtcEndpoint());
        } catch (Exception e) {
            log.error("Can't restore WebRtcEndpoint.");
            return null; // fixme
        }
    }

    public void saveSession(final WebSocketSession session, String roomId, String email) {
        try {
            sessionService.createSessionById(session.getId(), session, roomId, email);
            userService.updateSessionById(session.getId(), email); // notice: 여기서 세션id 저장
        } catch (ServiceException e) {
            log.error("Can't save websocket session.");
            // fixme: 후속 조치
        }
    }

    public void deleteSession(String sessionId) {
        try {
            Session userSession = sessionService.findSessionById(sessionId);
            String email = userSession.getEmail(); // 웹소켓 끊어진 사람의 email
            String roomId = userSession.getRoomId(); // 웹소켓 끊어진 사람이 속한 room
            if (email.equals(roomId)) { // notice: presenter이 끊김
                Set<String> viewers = roomService.findViewers(email); // 방에 속한 시청자들
                for (String viewerId : viewers) { // notice: viewerId는 이메일
                    User viewer = userService.findById(viewerId);
                    userService.leaveRoom(viewerId);
                    sessionService.deleteSessionById(viewer.getSessionId());
                }
                userService.leaveRoom(email);
                sessionService.deleteSessionById(sessionId);
                roomService.delete(email);
            } else { // notice: viewer가 끊김
                roomService.subViewer(roomId, email); // 방에서 뷰어 없애주고
                userService.leaveRoom(email);
                sessionService.deleteSessionById(sessionId);
            }
        } catch(ServiceException e) {
            log.error("WebSocket Session not exists.");
        }
    }

    public static void sendMessage(WebSocketSession session, JsonObject response) {
        try {
            session.sendMessage(new TextMessage(response.toString()));
        } catch (Exception e) {
            log.error("Can't send webSocket message.");
            // question: 웹소켓 연결이 끊어지면 이미 afterConnectionClosed 가 실행될것임
        }
    }
}
