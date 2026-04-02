package com.offlineqa.model;

import jakarta.validation.constraints.NotBlank;

public class ChatRequest {
    private String username;
    private String userId;
    @NotBlank
    private String kbId;
    @NotBlank
    private String question;
    private Long sessionId;

    public String getUsername() {
        if (username != null && !username.isBlank()) {
            return username;
        }
        return userId;
    }

    public void setUsername(String username) { this.username = username; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getKbId() { return kbId; }
    public void setKbId(String kbId) { this.kbId = kbId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
}
