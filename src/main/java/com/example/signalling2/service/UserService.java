package com.example.signalling2.service;

import com.example.signalling2.domain.UserSession;
import com.example.signalling2.exception.ServiceException;
import com.example.signalling2.exception.errcode.ServiceErrorCode;
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
        return memoryUserRepository.save(session).orElseThrow(()-> new ServiceException(ServiceErrorCode.NO_USER));
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
//        throw new ServiceException(ServiceErrorCode.NO_USER);
        return memoryUserRepository.findById(userId).orElseThrow(()-> new ServiceException(ServiceErrorCode.NO_USER));
    }

    public String findRoomId(String userId) {
        return findById(userId).getRoomId();
    }

}
