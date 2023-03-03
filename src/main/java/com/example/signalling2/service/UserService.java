package com.example.signalling2.service;

import com.example.signalling2.domain.User;
import com.example.signalling2.common.exception.ServiceException;
import com.example.signalling2.common.exception.errcode.ServiceErrorCode;
import com.example.signalling2.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final MediaService mediaService;

    public User createById(String email, String roomId, String endpoint) {
        if (userRepository.findById(email).isPresent()) {
            throw new ServiceException(ServiceErrorCode.ALREADY_IN);
        }
        User user = new User(email, roomId, endpoint);
        log.info("New user: " + email);
        return userRepository.save(user);
    }


    public void deleteById(String email) {
        if (userRepository.findById(email).isEmpty()) {
            throw new ServiceException(ServiceErrorCode.ALREADY_OUT);
        }
        log.info("Deleted user: " + email);
        userRepository.deleteById(email);
    }

    public User findById(String userId) {
        return userRepository.findById(userId).orElseThrow(()-> new ServiceException(ServiceErrorCode.NO_USER));
    }

    public String getSessionIdById(String userId) {
        User user = findById(userId);
        if (user.getSessionId() == null) {
            throw new ServiceException(ServiceErrorCode.NO_SESSION);
        }
        return user.getSessionId();
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
            mediaService.releaseEndpoint(user.getWebRtcEndpoint());
        }
    }
}
