package com.offlineqa.repository;

import com.offlineqa.model.ChatSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class SessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public SessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long create(Long userId, String kbId, String sessionName) {
        jdbcTemplate.update(
                "INSERT INTO chat_session(user_id, kb_id, session_name, is_delete, summary_text, created_at) VALUES (?, ?, ?, 0, '', ?)",
                userId,
                kbId,
                sessionName,
                Timestamp.valueOf(LocalDateTime.now())
        );
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public List<ChatSession> listByUser(Long userId) {
        return jdbcTemplate.query(
                "SELECT id, user_id, kb_id, session_name, is_delete, created_at FROM chat_session WHERE user_id = ? AND is_delete = 0 ORDER BY id DESC",
                (rs, rowNum) -> {
                    ChatSession session = new ChatSession();
                    session.setId(rs.getLong("id"));
                    session.setUserId(rs.getLong("user_id"));
                    session.setKbId(rs.getString("kb_id"));
                    session.setSessionName(rs.getString("session_name"));
                    session.setIsDelete(rs.getInt("is_delete"));
                    session.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    return session;
                },
                userId
        );
    }

    public void softDelete(Long userId, Long sessionId) {
        jdbcTemplate.update("UPDATE chat_session SET is_delete = 1 WHERE id = ? AND user_id = ?", sessionId, userId);
    }

    public Optional<String> getSummaryText(Long userId, Long sessionId) {
        var list = jdbcTemplate.query(
                "SELECT summary_text FROM chat_session WHERE id = ? AND user_id = ? AND is_delete = 0 LIMIT 1",
                (rs, rowNum) -> rs.getString("summary_text"),
                sessionId,
                userId
        );
        return list.stream().findFirst();
    }

    public void updateSummaryText(Long userId, Long sessionId, String summary) {
        jdbcTemplate.update(
                "UPDATE chat_session SET summary_text = ?, summary_updated_at = ? WHERE id = ? AND user_id = ?",
                summary,
                Timestamp.valueOf(LocalDateTime.now()),
                sessionId,
                userId
        );
    }
}
