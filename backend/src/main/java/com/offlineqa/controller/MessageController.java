package com.offlineqa.controller;

import com.offlineqa.controller.api.MessageApi;
import com.offlineqa.model.ChatMessage;
import com.offlineqa.service.MessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/message")
public class MessageController implements MessageApi {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    @GetMapping("/list")
    public List<ChatMessage> list(@RequestParam("username") String username,
                                  @RequestParam("sessionId") Long sessionId,
                                  @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return messageService.list(username, sessionId, limit);
    }
}
