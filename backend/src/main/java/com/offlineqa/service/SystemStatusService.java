package com.offlineqa.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SystemStatusService {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final WebClient qdrantClient;
    private final WebClient ollamaClient;
    private final boolean skipExternalServices;
    private final boolean volcSearchEnabled;
    private final String ollamaChatModel;
    private final String ollamaEmbeddingModel;

    public SystemStatusService(JdbcTemplate jdbcTemplate,
                               StringRedisTemplate redisTemplate,
                               @Value("${app.qdrant.base-url}") String qdrantBaseUrl,
                               @Value("${app.ollama.base-url}") String ollamaBaseUrl,
                               @Value("${app.dev.skip-external-services:false}") boolean skipExternalServices,
                               @Value("${app.volc.search-enabled:false}") boolean volcSearchEnabled,
                               @Value("${app.ollama.chat-model}") String ollamaChatModel,
                               @Value("${app.ollama.embedding-model}") String ollamaEmbeddingModel) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.qdrantClient = WebClient.builder().baseUrl(qdrantBaseUrl).build();
        this.ollamaClient = WebClient.builder().baseUrl(ollamaBaseUrl).build();
        this.skipExternalServices = skipExternalServices;
        this.volcSearchEnabled = volcSearchEnabled;
        this.ollamaChatModel = ollamaChatModel;
        this.ollamaEmbeddingModel = ollamaEmbeddingModel;
    }

    public Map<String, Object> status() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("database", checkDatabase());
        out.put("redis", checkRedis());
        out.put("qdrant", checkQdrant());
        out.put("ollama", checkOllama());
        out.put("volcSearchEnabled", volcSearchEnabled);
        out.put("skipExternalServices", skipExternalServices);
        out.put("chatModel", ollamaChatModel);
        out.put("embeddingModel", ollamaEmbeddingModel);
        return out;
    }

    private Map<String, Object> checkDatabase() {
        Map<String, Object> info = new LinkedHashMap<>();
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            info.put("ok", one != null && one == 1);
        } catch (Exception e) {
            info.put("ok", false);
            info.put("error", e.getMessage());
        }
        return info;
    }

    private Map<String, Object> checkRedis() {
        Map<String, Object> info = new LinkedHashMap<>();
        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            info.put("ok", "PONG".equalsIgnoreCase(pong));
        } catch (Exception e) {
            info.put("ok", false);
            info.put("error", e.getMessage());
        }
        return info;
    }

    private Map<String, Object> checkQdrant() {
        Map<String, Object> info = new LinkedHashMap<>();
        try {
            qdrantClient.get().uri("/collections").retrieve().bodyToMono(String.class).block();
            info.put("ok", true);
        } catch (Exception e) {
            info.put("ok", false);
            info.put("error", e.getMessage());
        }
        return info;
    }

    private Map<String, Object> checkOllama() {
        Map<String, Object> info = new LinkedHashMap<>();
        try {
            ollamaClient.get().uri("/api/tags").retrieve().bodyToMono(String.class).block();
            info.put("ok", true);
        } catch (Exception e) {
            info.put("ok", false);
            info.put("error", e.getMessage());
        }
        return info;
    }
}
