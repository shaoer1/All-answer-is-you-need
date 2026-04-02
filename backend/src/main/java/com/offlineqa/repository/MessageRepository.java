package com.offlineqa.repository;

import com.offlineqa.model.ChatMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class MessageRepository {

    private final JdbcTemplate jdbcTemplate;

    public MessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(Long sessionId, Long userId, String role, String messageContent) {
        jdbcTemplate.update(
                "INSERT INTO chat_message(session_id, user_id, role, message_content, created_at) VALUES (?, ?, ?, ?, ?)",
                sessionId,
                userId,
                role,
                messageContent,
                Timestamp.valueOf(LocalDateTime.now())
        );
    }

    public List<ChatMessage> listBySession(Long userId, Long sessionId, int limit) {
        return jdbcTemplate.query(
                "SELECT id, session_id, user_id, role, message_content, created_at FROM chat_message WHERE user_id = ? AND session_id = ? ORDER BY id DESC LIMIT ?",
                (rs, rowNum) -> mapMessage(rs.getLong("id"), rs.getLong("session_id"), rs.getLong("user_id"), rs.getString("role"), rs.getString("message_content"), rs.getTimestamp("created_at").toLocalDateTime()),
                userId,
                sessionId,
                limit
        );
    }

    public List<ChatMessage> listRecentAsc(Long userId, Long sessionId, int limit) {
        return jdbcTemplate.query(
                "SELECT id, session_id, user_id, role, message_content, created_at FROM (SELECT * FROM chat_message WHERE user_id = ? AND session_id = ? ORDER BY id DESC LIMIT ?) t ORDER BY id ASC",
                (rs, rowNum) -> mapMessage(rs.getLong("id"), rs.getLong("session_id"), rs.getLong("user_id"), rs.getString("role"), rs.getString("message_content"), rs.getTimestamp("created_at").toLocalDateTime()),
                userId,
                sessionId,
                limit
        );
    }

    public long countBySession(Long userId, Long sessionId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM chat_message WHERE user_id = ? AND session_id = ?",
                Long.class,
                userId,
                sessionId
        );
        return count == null ? 0 : count;
    }

    private ChatMessage mapMessage(Long id, Long sessionId, Long userId, String role, String content, LocalDateTime createdAt) {
        ChatMessage message = new ChatMessage();
        message.setId(id);
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(role);
        message.setMessageContent(content);
        message.setCreatedAt(createdAt);
        return message;
    }
}
