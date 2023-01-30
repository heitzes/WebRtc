package com.example.signalling2.service;

import com.example.signalling2.domain.UserSession;
import com.example.signalling2.repository.MemoryUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final MemoryUserRepository memoryUserRepository;

    public boolean isEmpty(){
        if (memoryUserRepository.getSequence()==0) {
            return true;
        }
        return false;
    }

    public UserSession save(UserSession session) {
        return memoryUserRepository.save(session).orElseThrow(()-> new RuntimeException("User session 저장 오류"));
    }
    public void leaveRoom(UserSession userSession) {
        if (userSession.getWebRtcEndpoint() != null) {
            userSession.getWebRtcEndpoint().release();
        }
        userSession.setWebRtcEndpoint(null);
        userSession.setMediaPipeline(null);
        userSession.setRoomId(null);
    }

    public void remove(String userId) {
        memoryUserRepository.delete(userId);
    }

    public void endLive(String userId) {
        UserSession user = this.findById(userId);

    }

    public UserSession findById(String userId) {
        return memoryUserRepository.findById(userId).orElseThrow(()-> new RuntimeException("UserSession 존재하지 않음"));
    }

    public String findRoomId(String userId) {
        return findById(userId).getRoomId();
    }

}
