package com.example.signalling2.service;

import com.example.signalling2.domain.UserSession;

public interface UserService {
    UserSession save(UserSession session);
    UserSession findById(String userId);
}
