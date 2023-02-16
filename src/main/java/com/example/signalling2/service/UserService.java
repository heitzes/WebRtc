package com.example.signalling2.service;

import com.example.signalling2.domain.SessionRedis;
import com.example.signalling2.domain.UserSession;
import com.example.signalling2.exception.ServiceException;
import com.example.signalling2.exception.errcode.ServiceErrorCode;
import com.example.signalling2.repository.MemoryUserRepository;
import com.example.signalling2.repository.RedisSessionRepository;
import lombok.RequiredArgsConstructor;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class UserService {
    private final MemoryUserRepository memoryUserRepository;
    // notice: WebSocket Connection 끊어졌을때 sessionId 보고 어느 User 인지 식별하기 위해
    private final RedisSessionRepository sessionRepository;
    private final ConcurrentHashMap<String, String> sessionMap = new ConcurrentHashMap<>();

    public UserSession createById(String email) {
        if (memoryUserRepository.findById(email).isPresent()){
            throw new ServiceException(ServiceErrorCode.ALREADY_IN);
        }
        UserSession user = new UserSession(email);
        return memoryUserRepository.save(user).orElseThrow(()-> new ServiceException(ServiceErrorCode.NO_USER));
    }

    public void deleteById(String email) {
        if (!memoryUserRepository.findById(email).isPresent()) {
            throw new ServiceException(ServiceErrorCode.NO_SESSION);
        }
        memoryUserRepository.delete(email);
    }

    public void deleteSessionById(String sessionId) {
        if (sessionRepository.findById(sessionId).isEmpty()) {
            throw new ServiceException(ServiceErrorCode.NO_SESSION);
        }
        sessionRepository.delete(sessionId);
    }
    public SessionRedis findSessionById(String sessionId) {
        return sessionRepository.findById(sessionId).orElseThrow(()-> new ServiceException(ServiceErrorCode.NO_SESSION));
    }
    public void updateById(WebRtcEndpoint ep, String roomId, String email) {
        UserSession user = findById(email);
        user.setWebRtcEndpoint(ep);
        user.setRoomId(roomId);
    }

    public void updateById(MediaPipeline pipeline, String email) {
        UserSession user = findById(email);
        user.setMediaPipeline(pipeline);
    }

    public void updateById(WebSocketSession session, String email) {
        UserSession user = findById(email);
        user.setSession(session);
        user.setSessionId(session.getId());
    }

    public void leaveRoom(String sessionId, String email) {
        releaseEndpoint(email);
        deleteById(email);
        deleteSessionById(sessionId);
    }

    public void deleteRoom(String sessionId, String email) {
//        releasePipeline(email);
        releaseEndpoint(email);
        deleteById(email);
        deleteSessionById(sessionId);
    }

    public void createSessionById(String sessionId, String roomId, String email) {
        SessionRedis sessionRedis = new SessionRedis(roomId, email);
        sessionRepository.save(sessionId, sessionRedis);
    }

    public UserSession findById(String userId) {
        return memoryUserRepository.findById(userId).orElseThrow(()-> new ServiceException(ServiceErrorCode.NO_USER));
    }

    public void releaseEndpoint(String email) {
        UserSession user = findById(email);
        if (user.getWebRtcEndpoint() != null) {
            user.getWebRtcEndpoint().release();
        }
    }

    public void releasePipeline(String email) {
        UserSession user = findById(email);
        if (user.getMediaPipeline() != null) {
            user.getMediaPipeline().release();
        }
    }

}
