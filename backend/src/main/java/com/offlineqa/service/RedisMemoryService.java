package com.offlineqa.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisMemoryService {

    private final StringRedisTemplate redisTemplate;

    public RedisMemoryService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveShortTerm(String username, Long sessionId, String content) {
        if (username == null || sessionId == null || content == null) {
            return;
        }
        String key = "mem:short:" + username + ":" + sessionId;
        try {
            redisTemplate.opsForValue().set(key, content, Duration.ofHours(4));
        } catch (Exception ignored) {
            // Redis is optional for local development; ignore cache failures.
        }
    }

    public String getShortTerm(String username, Long sessionId) {
        if (username == null || sessionId == null) {
            return "";
        }
        String key = "mem:short:" + username + ":" + sessionId;
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception ignored) {
            return "";
        }
    }
}
