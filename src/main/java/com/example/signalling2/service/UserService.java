package com.example.signalling2.service;

import com.example.signalling2.domain.User;
import com.example.signalling2.exception.ServiceException;
import com.example.signalling2.exception.errcode.ServiceErrorCode;
import com.example.signalling2.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final MediaService mediaService;

    public User createById(String email, String roomId) {
        if (userRepository.findById(email).isPresent()){
            throw new ServiceException(ServiceErrorCode.ALREADY_IN);
        }
        User user = new User(email, roomId);
        System.out.println("New user: " + email);
        return userRepository.save(user);
    }


    public void deleteById(String email) {
        if (userRepository.findById(email).isEmpty()) {
            throw new ServiceException(ServiceErrorCode.ALREADY_OUT);
        }
        userRepository.deleteById(email);
    }

    public User findById(String userId) {
        return userRepository.findById(userId).orElseThrow(()-> new ServiceException(ServiceErrorCode.NO_USER));
    }

    public void updateEndpointById(String endpoint, String email) {
        User user = findById(email);
        System.out.println("update endpoint: " + endpoint);
        user.setWebRtcEndpoint(endpoint);
        userRepository.save(user); // notice: update
    }

    public User updateSessionById(String sessionId, String email) {
        User user = findById(email);
        user.setSessionId(sessionId);
        return userRepository.save(user); // notice: update
    }

    public void leaveRoom(String email) {
        releaseEndpoint(email);
        deleteById(email);
    }

    public void releaseEndpoint(String email) {
        User user = findById(email);
        if (user.getWebRtcEndpoint() != null) {
            mediaService.releaseMedia(user.getWebRtcEndpoint());
        }
    }
}
