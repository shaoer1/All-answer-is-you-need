package com.offlineqa.controller;

import com.offlineqa.controller.api.UserApi;
import com.offlineqa.model.UserInitRequest;
import com.offlineqa.model.UserInitResponse;
import com.offlineqa.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController implements UserApi {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Override
    @PostMapping("/init")
    public UserInitResponse init(@Valid @RequestBody UserInitRequest request) {
        long userId = userService.initUser(request.getUsername());
        return new UserInitResponse(userId, request.getUsername());
    }
}
