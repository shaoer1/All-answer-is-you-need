package com.offlineqa.controller.api;

import com.offlineqa.model.UserAuthResponse;
import com.offlineqa.model.UserInitRequest;
import com.offlineqa.model.UserInitResponse;
import com.offlineqa.model.UserLoginRequest;
import com.offlineqa.model.UserRegisterRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Map;

@RequestMapping("/api/user")
public interface UserApi {
    @PostMapping("/init")
    UserInitResponse init(@Valid @RequestBody UserInitRequest request);
    @PostMapping("/register")
    UserAuthResponse register(@Valid @RequestBody UserRegisterRequest request);
    @PostMapping("/login")
    UserAuthResponse login(@Valid @RequestBody UserLoginRequest request);
    @GetMapping("/id")
    Map<String, Object> getUserId(@RequestParam("username") String username);
}
