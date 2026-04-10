package com.offlineqa.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SessionCreateRequest {
    @NotBlank
    private String username;
    @NotNull
    private Long kbId;
    @NotBlank
    private String sessionName;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Long getKbId() { return kbId; }
    public void setKbId(Long kbId) { this.kbId = kbId; }
    public String getSessionName() { return sessionName; }
    public void setSessionName(String sessionName) { this.sessionName = sessionName; }
}