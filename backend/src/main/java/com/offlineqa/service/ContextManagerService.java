package com.offlineqa.service;

import com.offlineqa.model.ChatMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContextManagerService {

    private final MessageService messageService;
    private final SessionService sessionService;
    private final RedisMemoryService redisMemoryService;

    @Value("${app.context.window-rounds:6}")
    private int windowRounds;

    @Value("${app.context.summary-trigger-messages:16}")
    private int summaryTriggerMessages;

    public ContextManagerService(MessageService messageService,
                                 SessionService sessionService,
                                 RedisMemoryService redisMemoryService) {
        this.messageService = messageService;
        this.sessionService = sessionService;
        this.redisMemoryService = redisMemoryService;
    }

    public String buildContext(String username, Long sessionId) {
        if (sessionId == null) {
            return "";
        }

        String summary = sessionService.getSummaryText(username, sessionId);
        List<ChatMessage> recentMessages = messageService.listRecentAsc(username, sessionId, windowRounds * 2);

        String recent = recentMessages.stream()
                .map(m -> "[" + m.getRole() + "] " + m.getMessageContent())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        redisMemoryService.saveShortTerm(username, sessionId, recent);

        if (summary == null || summary.isBlank()) {
            return recent;
        }
        return "[历史摘要]\n" + summary + "\n\n[最近对话]\n" + recent;
    }

    public void maybeRefreshSummary(String username, Long sessionId) {
        if (sessionId == null) {
            return;
        }
        long count = messageService.countBySession(username, sessionId);
        if (count < summaryTriggerMessages) {
            return;
        }

        List<ChatMessage> messages = messageService.listRecentAsc(username, sessionId, summaryTriggerMessages);
        String summary = messages.stream()
                .map(m -> "[" + m.getRole() + "] " + m.getMessageContent())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        if (summary.length() > 1200) {
            summary = summary.substring(0, 1200);
        }
        sessionService.updateSummaryText(username, sessionId, summary);
    }
}
