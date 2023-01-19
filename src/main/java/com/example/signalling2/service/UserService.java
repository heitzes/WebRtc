package com.example.signalling2.service;

import com.example.signalling2.domain.UserSession;
import com.example.signalling2.repository.MemoryUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    public void remove(String userId) {
        memoryUserRepository.delete(userId);
    }

    public UserSession findById(String userId) {
        return memoryUserRepository.findById(userId).orElseThrow(()-> new RuntimeException("UserSession 존재하지 않음"));
    }

    public String findRoomId(String userId) {
        return memoryUserRepository.findById(userId).get().getRoomId();
    }

}
