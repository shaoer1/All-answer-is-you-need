package com.offlineqa.service;

import com.offlineqa.model.ChunkRecord;
import com.offlineqa.model.UploadResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class IngestionService {

    private final DocumentParserService parserService;
    private final TextCleanerService cleanerService;
    private final AdaptiveChunkService chunkService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final UserService userService;

    public IngestionService(DocumentParserService parserService,
                            TextCleanerService cleanerService,
                            AdaptiveChunkService chunkService,
                            EmbeddingService embeddingService,
                            VectorStoreService vectorStoreService,
                            UserService userService) {
        this.parserService = parserService;
        this.cleanerService = cleanerService;
        this.chunkService = chunkService;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.userService = userService;
    }

    public UploadResponse upload(String principal, Long kbId, MultipartFile file) {
        if (principal == null || principal.isBlank()) {
            throw new IllegalArgumentException("username/userId 不能为空");
        }
        String userId = String.valueOf(userService.initUser(principal));

        String parsed = parserService.parse(file);
        String cleaned = cleanerService.clean(parsed);
        List<String> chunks = chunkService.split(cleaned);

        String docId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        List<String> ignoredHashes = new ArrayList<>();
        int saved = 0;

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            String hash = hash(chunk);
            if (vectorStoreService.existsByHash(userId, String.valueOf(kbId), hash)) {
                ignoredHashes.add(hash);
                continue;
            }
            List<Double> vector = embeddingService.embed(chunk);
            ChunkRecord record = new ChunkRecord();
            record.setUserId(userId);
            record.setKbId(String.valueOf(kbId));
            record.setDocId(docId);
            record.setChunkIndex(i);
            record.setContent(chunk);
            record.setEmbeddingJson("[]");
            record.setContentHash(hash);
            record.setCreatedAt(now);
            record.setLastAccessedAt(now);
            vectorStoreService.saveChunk(record, vector);
            saved++;
        }
        return new UploadResponse(docId, saved, ignoredHashes);
    }

    private String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}