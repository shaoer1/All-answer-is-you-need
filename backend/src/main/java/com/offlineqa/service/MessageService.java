package com.offlineqa.service;

import com.offlineqa.model.ChatMessage;
import com.offlineqa.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {

    private final UserService userService;
    private final MessageRepository messageRepository;

    public MessageService(UserService userService, MessageRepository messageRepository) {
        this.userService = userService;
        this.messageRepository = messageRepository;
    }

    public void save(Long sessionId, String username, String role, String content) {
        if (sessionId == null) {
            return;
        }
        long userId = userService.initUser(username);
        messageRepository.save(sessionId, userId, role, content);
    }

    public List<ChatMessage> list(String username, Long sessionId, int limit) {
        long userId = userService.initUser(username);
        return messageRepository.listBySession(userId, sessionId, limit);
    }

    public List<ChatMessage> listRecentAsc(String username, Long sessionId, int limit) {
        long userId = userService.initUser(username);
        return messageRepository.listRecentAsc(userId, sessionId, limit);
    }

    public long countBySession(String username, Long sessionId) {
        long userId = userService.initUser(username);
        return messageRepository.countBySession(userId, sessionId);
    }
}
