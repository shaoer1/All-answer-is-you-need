package com.offlineqa.controller;

import com.offlineqa.controller.api.SessionApi;
import com.offlineqa.model.ChatSession;
import com.offlineqa.model.BubbleState;
import com.offlineqa.model.BubbleStateSaveRequest;
import com.offlineqa.model.SessionCreateRequest;
import com.offlineqa.model.SessionCreateResponse;
import com.offlineqa.service.BubbleStateService;
import com.offlineqa.service.SessionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/session")
public class SessionController implements SessionApi {

    private final SessionService sessionService;
    private final BubbleStateService bubbleStateService;

    public SessionController(SessionService sessionService, BubbleStateService bubbleStateService) {
        this.sessionService = sessionService;
        this.bubbleStateService = bubbleStateService;
    }

    @Override
    @PostMapping("/create")
    public SessionCreateResponse create(@Valid @RequestBody SessionCreateRequest request) {
        long sessionId = sessionService.createSession(request.getUsername(), request.getKbId(), request.getSessionName());
        return new SessionCreateResponse(sessionId);
    }

    @Override
    @GetMapping("/list")
    public List<ChatSession> list(@RequestParam("username") String username) {
        return sessionService.listSessions(username);
    }

    @Override
    @DeleteMapping("/delete")
    public void delete(@RequestParam("username") String username, @RequestParam("sessionId") Long sessionId) {
        sessionService.deleteSession(username, sessionId);
    }

    @Override
    @PostMapping("/update-name")
    public void updateName(@RequestParam("username") String username, @RequestParam("sessionId") Long sessionId, @RequestParam("name") String name) {
        sessionService.updateSessionName(username, sessionId, name);
    }

    @GetMapping("/bubbles")
    public List<BubbleState> listBubbles(@RequestParam("username") String username, @RequestParam("sessionId") Long sessionId) {
        return bubbleStateService.list(username, sessionId);
    }

    @PostMapping("/bubbles/save")
    public void saveBubbles(@Valid @RequestBody BubbleStateSaveRequest request) {
        bubbleStateService.save(request.getUsername(), request.getSessionId(), request.getStates());
    }
}
