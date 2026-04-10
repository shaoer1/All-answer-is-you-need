package com.offlineqa.repository;

import com.offlineqa.model.ChunkRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ChunkRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<ChunkRecord> rowMapper = (rs, rowNum) -> {
        ChunkRecord record = new ChunkRecord();
        record.setId(rs.getLong("id"));
        record.setUserId(rs.getString("user_id"));
        record.setKbId(rs.getString("kb_id"));
        record.setDocId(rs.getString("doc_id"));
        record.setChunkIndex(rs.getInt("chunk_index"));
        record.setContent(rs.getString("content"));
        record.setEmbeddingJson(rs.getString("embedding_json"));
        record.setContentHash(rs.getString("content_hash"));
        record.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        record.setLastAccessedAt(rs.getTimestamp("last_accessed_at").toLocalDateTime());
        return record;
    };

    public ChunkRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(ChunkRecord record) {
        jdbcTemplate.update("""
                INSERT INTO chunk_record(user_id, kb_id, doc_id, chunk_index, content, embedding_json, content_hash, created_at, last_accessed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                record.getUserId(),
                record.getKbId(),
                record.getDocId(),
                record.getChunkIndex(),
                record.getContent(),
                record.getEmbeddingJson(),
                record.getContentHash(),
                Timestamp.valueOf(record.getCreatedAt()),
                Timestamp.valueOf(record.getLastAccessedAt())
        );
    }

    public boolean existsByHash(String userId, String kbId, String contentHash) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM chunk_record WHERE user_id = ? AND kb_id = ? AND content_hash = ?",
                Integer.class,
                userId,
                kbId,
                contentHash
        );
        return count != null && count > 0;
    }

    public List<ChunkRecord> findByScope(String userId, String kbId) {
        return jdbcTemplate.query(
                "SELECT * FROM chunk_record WHERE user_id = ? AND kb_id = ?",
                rowMapper,
                userId,
                kbId
        );
    }

    public List<ChunkRecord> findRecentByScope(String userId, String kbId, int limit) {
        int safeLimit = Math.max(20, Math.min(limit, 500));
        return jdbcTemplate.query(
                "SELECT * FROM chunk_record WHERE user_id = ? AND kb_id = ? ORDER BY last_accessed_at DESC LIMIT ?",
                rowMapper,
                userId,
                kbId,
                safeLimit
        );
    }

    public List<ChunkRecord> findByKeywords(String userId, String kbId, List<String> keywords, int limit) {
        if (keywords == null || keywords.isEmpty()) {
            return List.of();
        }
        int safeLimit = Math.max(10, Math.min(limit, 200));
        StringBuilder sql = new StringBuilder("SELECT * FROM chunk_record WHERE user_id = ? AND kb_id = ? AND (");
        List<Object> args = new ArrayList<>();
        args.add(userId);
        args.add(kbId);
        for (int i = 0; i < keywords.size(); i++) {
            if (i > 0) sql.append(" OR ");
            sql.append("content LIKE ?");
            args.add("%" + keywords.get(i) + "%");
        }
        sql.append(") ORDER BY last_accessed_at DESC LIMIT ?");
        args.add(safeLimit);
        return jdbcTemplate.query(sql.toString(), rowMapper, args.toArray());
    }

    public void touchScope(String userId, String kbId, LocalDateTime now) {
        jdbcTemplate.update(
                "UPDATE chunk_record SET last_accessed_at = ? WHERE user_id = ? AND kb_id = ?",
                Timestamp.valueOf(now),
                userId,
                kbId
        );
    }

    public int deleteExpired(LocalDateTime expiredBefore) {
        return jdbcTemplate.update(
                "DELETE FROM chunk_record WHERE last_accessed_at < ?",
                Timestamp.valueOf(expiredBefore)
        );
    }
}
