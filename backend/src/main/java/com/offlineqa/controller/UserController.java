package com.offlineqa.controller;

import com.offlineqa.controller.api.UserApi;
import com.offlineqa.model.*;
import com.offlineqa.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class UserController implements UserApi {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserInitResponse init(@Valid @RequestBody UserInitRequest request) {
        long userId = userService.initUser(request.getUsername());
        return new UserInitResponse(userId, request.getUsername());
    }

    @Override
    public UserAuthResponse register(@Valid @RequestBody UserRegisterRequest request) {
        SysUser user = userService.register(request.getUsername(), request.getPassword(), request.getNickname());
        return new UserAuthResponse(user.getId(), user.getUsername(), user.getNickname());
    }

    @Override
    public UserAuthResponse login(@Valid @RequestBody UserLoginRequest request) {
        SysUser user = userService.login(request.getUsername(), request.getPassword());
        return new UserAuthResponse(user.getId(), user.getUsername(), user.getNickname());
    }

    @Override
    public Map<String, Object> getUserId(@RequestParam("username") String username) {
        System.out.println("getUserId called with username: " + username);
        try {
            long userId = userService.initUser(username);
            System.out.println("initUser returned userId: " + userId);
            Map<String, Object> response = Map.of(
                "userId", userId,
                "username", username,
                "nickname", username
            );
            System.out.println("Created response: " + response);
            return response;
        } catch (Exception e) {
            System.out.println("Error in getUserId: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}