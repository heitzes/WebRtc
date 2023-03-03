package com.example.signalling2.utils;

import com.example.signalling2.common.exception.KurentoException;
import com.example.signalling2.domain.Room;
import com.example.signalling2.domain.User;
import com.example.signalling2.domain.Session;
import com.example.signalling2.common.exception.ServiceException;
import com.example.signalling2.service.MediaService;
import com.example.signalling2.service.RoomService;
import com.example.signalling2.service.SessionService;
import com.example.signalling2.service.UserService;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.K;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;

/**
 * WebSocketHandler 에서
 * 직접 service 관련 로직을 실행시 발생할 수 있는 exception을
 * WebSocketHandler에서 처리하지 않으면 자동으로 ExceptionWebSocketHandlerDecorator 클래스로 전파되기 때문에
 * 실행해야할 service 관련 로직은 이 클래스를 통해 실행하고,
 * 이 클래스에서 exception을 catch 합니다.
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
        } catch (ServiceException e1) {
            log.error("[ERROR] " + e1.getServiceErrorCode().getMessage());
            return null;
        } catch (KurentoException e2) {
            log.error("[ERROR] " + e2.getKurentoErrCode().getMessage());
            return replaceEndpoint(email);
        }
    }

    /**
     * endpoint 복구 불가시
     * 새로 생성하여 대체함
     *
     * 파이프라인 마저 복구 안되면 null 반환하여
     * webSocketHandler에서 closeConnection 실행하여
     * 방이랑 유저 다 삭제하고 사내 메신저로 Error 메세지 발송
     *
     * @param email
     * @return
     */
    public WebRtcEndpoint replaceEndpoint(String email) {
        try {
            User user = userService.findById(email);
            Room room = roomService.findById(user.getRoomId());
            MediaPipeline pipeline = mediaService.getPipeline(room.getMediaPipeline());
            WebRtcEndpoint endpoint = new WebRtcEndpoint.Builder(pipeline).build();
            user.setWebRtcEndpoint(endpoint.getId());
            return endpoint;
        } catch (ServiceException e1) {
            log.error("[ERROR] " + e1.getServiceErrorCode().getMessage());
            return null;
        } catch (KurentoException e2) {
            log.error("[ERROR] " + e2.getKurentoErrCode().getMessage());
            mediaService.sendToSmileHub("**ERROR** " + e2.getKurentoErrCode().getMessage());
            return null;
        }
    }

    public void saveSession(final WebSocketSession session, String roomId, String email) {
        try {
            sessionService.createSessionById(session.getId(), session, roomId, email);
            userService.updateSessionById(session.getId(), email); // notice: 여기서 세션id 저장
        } catch (Exception e) {
            log.error("[ERROR] " + e.getMessage());
        }
    }

    public Boolean sessionExist(String sessionId) {
        try {
            sessionService.findSessionById(sessionId);
            return true;
        } catch (ServiceException e) {
            log.error("[ERROR] " + e.getServiceErrorCode().getMessage());
            return false;
        }
    }

    public void findAndDeleteArtistSession(WebSocketSession session) {
        String sessionId = session.getId();
        try {
            String roomId = sessionService.findSessionById(sessionId).getRoomId(); // 웹소켓 끊어진 사람이 속한 room
            User artist = userService.findById(roomId);
            String artistSessionId = artist.getSessionId();
            WebSocketSession artistSession = sessionService.findSessionById(artistSessionId).getSession();
            deleteSession(artistSession);
        } catch (ServiceException e) {
            log.error("[ERROR] " + e.getServiceErrorCode().getMessage());
        }
    }

    public void deleteSession(WebSocketSession session) {
        String sessionId = session.getId();
        JsonObject response = ResponseUtil.messageResponse("stop", "error");
        try {
            Session userSession = sessionService.findSessionById(sessionId);
            String email = userSession.getEmail(); // 웹소켓 끊어진 사람의 email
            String roomId = userSession.getRoomId(); // 웹소켓 끊어진 사람이 속한 room
            if (email.equals(roomId)) { // notice: presenter이 끊김
                deletePresenterSession(email, sessionId);
            } else { // notice: viewer가 끊김
                deleteViewerSession(email, roomId, sessionId);
            }
        } catch(ServiceException e) {
            log.error("[ERROR] " + e.getServiceErrorCode().getMessage());
        } finally {
            if (session.isOpen()) {
                synchronized (session) {
                    sendMessage(session, response);
                }
            }
        }
    }

    public void deletePresenterSession(String email, String sessionId) {
        try {
            Set<String> viewers = roomService.findViewers(email); // 방에 속한 시청자들
            for (String viewerId : viewers) { // notice: viewerId는 이메일
                User viewer = userService.findById(viewerId);
                userService.leaveRoom(viewerId);
                sessionService.deleteSessionById(viewer.getSessionId());
            }
            userService.leaveRoom(email);
            sessionService.deleteSessionById(sessionId);
            roomService.delete(email);
        } catch (ServiceException e) {
            log.error("[ERROR] " + e.getServiceErrorCode().getMessage());
        }
    }

    public void deleteViewerSession(String email, String roomId, String sessionId) {
        try {
            roomService.subViewer(roomId, email); // 방에서 뷰어 없애주고
            userService.leaveRoom(email);
            sessionService.deleteSessionById(sessionId);
        } catch (ServiceException e) {
            log.error("[ERROR] " + e.getServiceErrorCode().getMessage());
        }
    }

    public static void sendMessage(WebSocketSession session, JsonObject response) {
        try {
            session.sendMessage(new TextMessage(response.toString()));
        } catch (Exception e) {
            log.error("[ERROR] " + e.getMessage());
        }
    }
}
