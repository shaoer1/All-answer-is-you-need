package com.offlineqa.controller.api;

import com.offlineqa.model.ChatMessage;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface MessageApi {
    List<ChatMessage> list(@RequestParam("username") String username,
                           @RequestParam("sessionId") Long sessionId,
                           @RequestParam(value = "limit", defaultValue = "50") int limit);
}
