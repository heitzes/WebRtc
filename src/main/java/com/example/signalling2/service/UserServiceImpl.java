package com.example.signalling2.service;

import com.example.signalling2.domain.UserSession;
import com.example.signalling2.repository.UserRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRedisRepository userRedisRepository;

    @Override
    public UserSession save(UserSession session) {
        return userRedisRepository.save(session);
    }

    @Override
    public UserSession findById(String userId) {
        return userRedisRepository.findById(userId).get();
    }
}
