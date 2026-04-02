package com.offlineqa.controller;

import com.offlineqa.controller.api.SessionApi;
import com.offlineqa.model.ChatSession;
import com.offlineqa.model.SessionCreateRequest;
import com.offlineqa.model.SessionCreateResponse;
import com.offlineqa.service.SessionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/session")
public class SessionController implements SessionApi {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    @PostMapping("/create")
    public SessionCreateResponse create(@Valid @RequestBody SessionCreateRequest request) {
        long sessionId = sessionService.createSession(request.getUsername(), request.getKbId(), request.getSessionName());
        return new SessionCreateResponse(sessionId);
    }

    @Override
    @GetMapping("/list")
    public List<ChatSession> list(@RequestParam String username) {
        return sessionService.listSessions(username);
    }

    @Override
    @DeleteMapping("/delete")
    public void delete(@RequestParam String username, @RequestParam Long sessionId) {
        sessionService.deleteSession(username, sessionId);
    }
}
