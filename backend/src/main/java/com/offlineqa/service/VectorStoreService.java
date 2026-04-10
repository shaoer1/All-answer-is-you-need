package com.offlineqa.service;

import com.offlineqa.model.ChunkRecord;
import com.offlineqa.model.RetrievedChunk;
import com.offlineqa.repository.ChunkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class VectorStoreService {

    private final ChunkRepository chunkRepository;
    private final QdrantService qdrantService;

    @Value("${app.rag.max-top-k}")
    private int maxTopK;

    @Value("${app.rag.similarity-threshold}")
    private double similarityThreshold;

    public VectorStoreService(ChunkRepository chunkRepository, QdrantService qdrantService) {
        this.chunkRepository = chunkRepository;
        this.qdrantService = qdrantService;
    }

    public void saveChunk(ChunkRecord record, List<Double> vector) {
        chunkRepository.save(record);
        qdrantService.upsert(
                record.getUserId(),
                record.getKbId(),
                record.getContent(),
                record.getContentHash(),
                vector,
                record.getLastAccessedAt()
        );
    }

    public boolean existsByHash(String userId, String kbId, String contentHash) {
        return chunkRepository.existsByHash(userId, kbId, contentHash);
    }

    public List<RetrievedChunk> retrieve(String userId, String kbId, List<Double> queryVector, int topK) {
        int limit = Math.min(topK, maxTopK);
        List<RetrievedChunk> chunks = qdrantService.search(userId, kbId, queryVector, limit, similarityThreshold);
        chunkRepository.touchScope(userId, kbId, LocalDateTime.now());
        return chunks;
    }

    public List<RetrievedChunk> retrieveHybrid(String userId, String kbId, List<Double> queryVector, List<String> keywords, int topK) {
        int limit = Math.min(topK, maxTopK);
        Map<String, RetrievedChunk> merged = new LinkedHashMap<>();

        if (qdrantService.isHealthy()) {
            List<RetrievedChunk> vectorHits = qdrantService.search(userId, kbId, queryVector, Math.max(limit, 8), similarityThreshold);
            for (RetrievedChunk hit : vectorHits) {
                RetrievedChunk existing = merged.get(hit.content());
                if (existing == null || hit.score() > existing.score()) {
                    merged.put(hit.content(), new RetrievedChunk(hit.content(), hit.score(), "vector"));
                }
            }
        }

        List<ChunkRecord> keywordHits = chunkRepository.findByKeywords(userId, kbId, keywords, 80);
        for (ChunkRecord row : keywordHits) {
            double lexicalScore = keywordScore(row.getContent(), keywords);
            RetrievedChunk existing = merged.get(row.getContent());
            if (existing == null) {
                merged.put(row.getContent(), new RetrievedChunk(row.getContent(), lexicalScore, "keyword"));
            } else {
                double mergedScore = Math.max(existing.score(), lexicalScore);
                String mergedSource = existing.source().contains("vector") ? "vector+keyword" : "keyword";
                merged.put(row.getContent(), new RetrievedChunk(row.getContent(), mergedScore, mergedSource));
            }
        }

        // 不再使用 recent-fallback。该兜底会在“完全不相关的问题”下引入错误证据，
        // 导致例如查询 LPL 赛程时命中到沐浴露内容。

        List<RetrievedChunk> result = merged.values().stream()
                .sorted(Comparator.comparingDouble(RetrievedChunk::score).reversed())
                .limit(limit)
                .toList();

        chunkRepository.touchScope(userId, kbId, LocalDateTime.now());
        return result;
    }

    public boolean isVectorHealthy() {
        return qdrantService.isHealthy();
    }

    private double keywordScore(String content, List<String> keywords) {
        if (content == null || content.isBlank() || keywords == null || keywords.isEmpty()) {
            return 0.0D;
        }
        String normalized = content.toLowerCase(Locale.ROOT);
        int matched = 0;
        for (String k : keywords) {
            if (k == null || k.isBlank()) continue;
            if (normalized.contains(k.toLowerCase(Locale.ROOT))) matched++;
        }
        if (matched == 0) return 0.0D;
        return 0.2D + Math.min(0.35D, matched * 0.08D);
    }

    public void cleanupExpired(LocalDateTime expiredBefore) {
        chunkRepository.deleteExpired(expiredBefore);
        qdrantService.deleteExpired(expiredBefore);
    }
}
