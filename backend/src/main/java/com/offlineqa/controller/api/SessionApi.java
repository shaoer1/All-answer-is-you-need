package com.offlineqa.controller.api;

import com.offlineqa.model.ChatSession;
import com.offlineqa.model.SessionCreateRequest;
import com.offlineqa.model.SessionCreateResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface SessionApi {
    SessionCreateResponse create(@Valid @RequestBody SessionCreateRequest request);

    List<ChatSession> list(@RequestParam("username") String username);

    void delete(@RequestParam("username") String username, @RequestParam("sessionId") Long sessionId);

    void updateName(@RequestParam("username") String username, @RequestParam("sessionId") Long sessionId, @RequestParam("name") String name);
}
