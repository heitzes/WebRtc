package com.example.signalling2.service;

import com.example.signalling2.domain.UserSession;
import com.example.signalling2.exception.ServiceException;
import com.example.signalling2.exception.errcode.ServiceErrorCode;
import com.example.signalling2.repository.MemoryUserRepository;
import lombok.RequiredArgsConstructor;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

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

    public UserSession createById(String email) {
        if (memoryUserRepository.findById(email).isPresent()){
            throw new ServiceException(ServiceErrorCode.ALREADY_IN);
        }
        UserSession user = new UserSession(email);
        return memoryUserRepository.save(user).orElseThrow(()-> new ServiceException(ServiceErrorCode.NO_USER));
    }

    public void deleteById(String email) {
        if (!memoryUserRepository.findById(email).isPresent()) {
            throw new ServiceException(ServiceErrorCode.ALREADY_OUT);
        }
        memoryUserRepository.delete(email);
    }

    public void updateById(String email, MediaPipeline pipeline, WebRtcEndpoint ep) {
        UserSession user = findById(email);
        user.setMediaPipeline(pipeline);
        user.setWebRtcEndpoint(ep);
        user.setRoomId(email);
    }

    public void updateById(WebSocketSession session, String email) {
        UserSession user = findById(email);
        user.setSession(session);
    }
    public void releaseViewer(UserSession userSession, String email) {
        if (userSession.getWebRtcEndpoint() != null) {
            userSession.getWebRtcEndpoint().release();
        }
        memoryUserRepository.delete(email);
    }

    public void releasePresenter(UserSession userSession, String email) {
        if (userSession.getMediaPipeline() != null) {
            userSession.getMediaPipeline().release();
        }
        if (userSession.getWebRtcEndpoint() != null) {
            userSession.getWebRtcEndpoint().release();
        }
        memoryUserRepository.delete(email);
    }

    public void endLive(String userId) {
        UserSession user = this.findById(userId);

    }

    public UserSession findById(String userId) {
        return memoryUserRepository.findById(userId).orElseThrow(()-> new ServiceException(ServiceErrorCode.NO_USER));
    }

    public String findRoomId(String userId) {
        return findById(userId).getRoomId();
    }

}
