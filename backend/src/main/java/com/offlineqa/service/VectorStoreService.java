package com.offlineqa.service;

import com.offlineqa.model.ChunkRecord;
import com.offlineqa.model.RetrievedChunk;
import com.offlineqa.repository.ChunkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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

    public void cleanupExpired(LocalDateTime expiredBefore) {
        chunkRepository.deleteExpired(expiredBefore);
        qdrantService.deleteExpired(expiredBefore);
    }
}
