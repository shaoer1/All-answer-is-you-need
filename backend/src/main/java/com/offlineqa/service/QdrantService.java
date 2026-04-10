package com.offlineqa.service;

import com.offlineqa.model.RetrievedChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class QdrantService {

    private final WebClient webClient;
    private final String collectionName;
    private final AtomicInteger vectorSize = new AtomicInteger(0);
    private final boolean skipExternalServices;

    public QdrantService(@Value("${app.qdrant.base-url}") String baseUrl,
                         @Value("${app.qdrant.collection}") String collectionName,
                         @Value("${app.dev.skip-external-services:false}") boolean skipExternalServices) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.collectionName = collectionName;
        this.skipExternalServices = skipExternalServices;
    }

    public void upsert(String userId, String kbId, String content, String contentHash, List<Double> vector, LocalDateTime lastAccessedAt) {
        if (skipExternalServices) {
            return;
        }
        
        try {
            ensureCollection(vector.size());
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("kbId", kbId);
            payload.put("content", content);
            payload.put("contentHash", contentHash);
            payload.put("lastAccessedAt", toEpoch(lastAccessedAt));

            Map<String, Object> point = Map.of(
                    "id", System.currentTimeMillis(),
                    "vector", vector,
                    "payload", payload
            );

            webClient.put()
                    .uri("/collections/{name}/points", collectionName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("points", List.of(point)))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            System.err.println("Failed to upsert vector: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<RetrievedChunk> search(String userId, String kbId, List<Double> queryVector, int topK, double scoreThreshold) {
        if (skipExternalServices) {
            return new ArrayList<>();
        }
        
        try {
            ensureCollection(queryVector.size());
            Map<String, Object> filter = scopeFilter(userId, kbId);
            Map<String, Object> body = new HashMap<>();
            body.put("vector", queryVector);
            body.put("limit", topK);
            body.put("with_payload", true);
            body.put("score_threshold", scoreThreshold);
            body.put("filter", filter);

            Map<?, ?> response = webClient.post()
                    .uri("/collections/{name}/points/search", collectionName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<RetrievedChunk> out = new ArrayList<>();
            if (response == null) {
                return out;
            }
            List<Map<String, Object>> result = (List<Map<String, Object>>) response.get("result");
            if (result == null) {
                return out;
            }
            for (Map<String, Object> item : result) {
                Number score = (Number) item.get("score");
                Map<String, Object> payload = (Map<String, Object>) item.get("payload");
                if (payload != null && payload.get("content") != null) {
                    out.add(new RetrievedChunk(payload.get("content").toString(), score.doubleValue()));
                }
            }
            return out;
        } catch (Exception e) {
            System.err.println("Failed to search vectors: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void deleteExpired(LocalDateTime expiredBefore) {
        Map<String, Object> filter = Map.of(
                "must", List.of(
                        Map.of("key", "lastAccessedAt", "range", Map.of("lt", toEpoch(expiredBefore)))
                )
        );
        webClient.post()
                .uri("/collections/{name}/points/delete", collectionName)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("filter", filter))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public boolean isHealthy() {
        if (skipExternalServices) {
            return true;
        }
        try {
            webClient.get()
                    .uri("/collections/{name}", collectionName)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private void ensureCollection(int size) {
        if (skipExternalServices) {
            return;
        }
        
        if (vectorSize.get() == size) {
            return;
        }

        Integer existingSize = readCollectionVectorSize();
        if (existingSize != null && existingSize != size) {
            // 向量大小不匹配，记录警告信息但不删除集合
            System.err.println("Warning: Vector size mismatch. Expected " + size + ", but collection has " + existingSize);
            // 不删除集合，使用现有的向量大小
            vectorSize.set(existingSize);
            return;
        }

        if (existingSize == null) {
            Map<String, Object> body = Map.of(
                    "vectors", Map.of("size", size, "distance", "Cosine")
            );
            webClient.put()
                    .uri("/collections/{name}", collectionName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        }
        vectorSize.set(size);
    }

    @SuppressWarnings("unchecked")
    private Integer readCollectionVectorSize() {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/collections/{name}", collectionName)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (response == null) {
                return null;
            }
            Map<String, Object> result = (Map<String, Object>) response.get("result");
            if (result == null) {
                return null;
            }
            Map<String, Object> config = (Map<String, Object>) result.get("config");
            if (config == null) {
                return null;
            }
            Map<String, Object> params = (Map<String, Object>) config.get("params");
            if (params == null) {
                return null;
            }
            Object vectors = params.get("vectors");
            if (!(vectors instanceof Map<?, ?> vectorsMap)) {
                return null;
            }
            Object sizeObj = vectorsMap.get("size");
            if (sizeObj instanceof Number n) {
                return n.intValue();
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private Map<String, Object> scopeFilter(String userId, String kbId) {
        return Map.of(
                "must", List.of(
                        Map.of("key", "userId", "match", Map.of("value", userId)),
                        Map.of("key", "kbId", "match", Map.of("value", kbId))
                )
        );
    }

    private long toEpoch(LocalDateTime time) {
        return time.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
