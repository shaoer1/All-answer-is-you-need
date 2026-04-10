package com.offlineqa.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BubbleStateSaveRequest {
    @NotBlank
    private String username;

    @NotNull
    private Long sessionId;

    @NotNull
    private List<BubbleState> states = new ArrayList<>();

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public List<BubbleState> getStates() {
        return states;
    }

    public void setStates(List<BubbleState> states) {
        this.states = states;
    }
}
