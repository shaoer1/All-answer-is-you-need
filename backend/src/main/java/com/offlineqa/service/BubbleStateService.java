package com.offlineqa.service;

import com.offlineqa.model.BubbleState;
import com.offlineqa.repository.BubbleStateRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BubbleStateService {

    private final UserService userService;
    private final BubbleStateRepository bubbleStateRepository;

    public BubbleStateService(UserService userService, BubbleStateRepository bubbleStateRepository) {
        this.userService = userService;
        this.bubbleStateRepository = bubbleStateRepository;
    }

    public List<BubbleState> list(String username, Long sessionId) {
        long userId = userService.initUser(username);
        return bubbleStateRepository.list(userId, sessionId);
    }

    public void save(String username, Long sessionId, List<BubbleState> states) {
        long userId = userService.initUser(username);
        for (BubbleState state : states) {
            if (state == null || state.getPairId() == null || state.getPairId().isBlank()) {
                continue;
            }
            bubbleStateRepository.upsert(userId, sessionId, state);
        }
    }
}
