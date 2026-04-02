package com.offlineqa.controller;

import com.offlineqa.model.ChatRequest;
import com.offlineqa.service.QaService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final QaService qaService;

    public ChatController(QaService qaService) {
        this.qaService = qaService;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody ChatRequest request) {
        return qaService.streamAnswer(request.getUsername(), request.getKbId(), request.getSessionId(), request.getQuestion());
    }
}
