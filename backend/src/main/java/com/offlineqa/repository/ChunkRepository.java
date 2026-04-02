package com.offlineqa.repository;

import com.offlineqa.model.ChunkRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
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
