package com.offlineqa.controller.api;

import com.offlineqa.model.UserInitRequest;
import com.offlineqa.model.UserInitResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

public interface UserApi {
    UserInitResponse init(@Valid @RequestBody UserInitRequest request);
}
