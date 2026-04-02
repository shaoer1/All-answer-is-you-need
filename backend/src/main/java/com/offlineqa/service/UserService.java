package com.offlineqa.service;

import com.offlineqa.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public long initUser(String username) {
        return userRepository.createIfAbsent(username);
    }
}
