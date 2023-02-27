package com.example.signalling2.service;

import com.example.signalling2.domain.Session;
import com.example.signalling2.common.exception.ServiceException;
import com.example.signalling2.common.exception.errcode.ServiceErrorCode;
import com.example.signalling2.repository.MemorySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

/**
 * WebSocketSession 은
 * 어쩔 수 없이 시그널 서버 in-memory 에서 관리해야 합니다.
 * - 직렬화하여 Redis DB에 저장이 불가능하고,
 * - 미디어 서버 객체와 달리 복원하는 방법도 없기 때문입니다.
 */
@Service
@RequiredArgsConstructor
public class SessionService {
    private final MemorySessionRepository sessionRepository;

    public void createSessionById(String sessionId, WebSocketSession webSocketSession, String roomId, String email) {
        Session session = new Session(sessionId, webSocketSession, roomId, email);
        sessionRepository.save(session);
    }

    public Session findSessionById(String sessionId) {
        if (sessionId == null) {
            throw new ServiceException(ServiceErrorCode.NO_SESSION); // api로 삭제 요청시 (세션이 없음)
        }
        return sessionRepository.findById(sessionId).orElseThrow(()-> new ServiceException(ServiceErrorCode.NO_SESSION));
    }

    public void deleteSessionById(String sessionId) {
        if (sessionId == null) {
            throw new ServiceException(ServiceErrorCode.NO_SESSION); // api로 삭제 요청시 (세션이 없음)
        }
        if (sessionRepository.findById(sessionId).isEmpty()) {
            throw new ServiceException(ServiceErrorCode.NO_SESSION); // sessionId는 있지만 value가 없을때
        }
        sessionRepository.delete(sessionId);
    }

}
