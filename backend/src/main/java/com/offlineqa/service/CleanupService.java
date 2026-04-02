package com.offlineqa.service;

import com.offlineqa.repository.ChunkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CleanupService {

    private final VectorStoreService vectorStoreService;

    @Value("${app.rag.idle-user-expire-minutes}")
    private long idleExpireMinutes;

    public CleanupService(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    @Scheduled(fixedDelay = 600000)
    public void recycleIdleScopeData() {
        LocalDateTime expireBefore = LocalDateTime.now().minusMinutes(idleExpireMinutes);
        vectorStoreService.cleanupExpired(expireBefore);
    }
}
