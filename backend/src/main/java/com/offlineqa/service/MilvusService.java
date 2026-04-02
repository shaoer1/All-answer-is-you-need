package com.offlineqa.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MilvusService {

    @Value("${app.milvus.enabled:false}")
    private boolean enabled;

    @Value("${app.milvus.uri:}")
    private String uri;

    public boolean isEnabled() {
        return enabled;
    }

    public String getUri() {
        return uri;
    }
}
