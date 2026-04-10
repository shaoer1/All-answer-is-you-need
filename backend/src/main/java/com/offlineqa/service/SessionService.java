package com.offlineqa.service;

import com.offlineqa.model.ChatSession;
import com.offlineqa.repository.SessionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SessionService {

    private final UserService userService;
    private final SessionRepository sessionRepository;

    public SessionService(UserService userService, SessionRepository sessionRepository) {
        this.userService = userService;
        this.sessionRepository = sessionRepository;
    }

    public long createSession(String username, Long kbId, String sessionName) {
        long userId = userService.initUser(username);
        return sessionRepository.create(userId, kbId, sessionName);
    }

    public List<ChatSession> listSessions(String username) {
        long userId = userService.initUser(username);
        return sessionRepository.listByUser(userId);
    }

    public void deleteSession(String username, Long sessionId) {
        long userId = userService.initUser(username);
        sessionRepository.softDelete(userId, sessionId);
    }

    public String getSummaryText(String username, Long sessionId) {
        long userId = userService.initUser(username);
        return sessionRepository.getSummaryText(userId, sessionId).orElse("");
    }

    public void updateSummaryText(String username, Long sessionId, String summaryText) {
        long userId = userService.initUser(username);
        sessionRepository.updateSummaryText(userId, sessionId, summaryText);
    }

    public void updateSessionName(String username, Long sessionId, String name) {
        long userId = userService.initUser(username);
        sessionRepository.updateName(userId, sessionId, name);
    }
}