package com.offlineqa.controller.api;

import com.offlineqa.model.ChatMessage;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface MessageApi {
    List<ChatMessage> list(@RequestParam String username, @RequestParam Long sessionId, @RequestParam(defaultValue = "50") int limit);
}
