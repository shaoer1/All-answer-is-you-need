package com.offlineqa.repository;

import com.offlineqa.model.KnowledgeBase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class KnowledgeBaseRepository {

    private final JdbcTemplate jdbcTemplate;

    public KnowledgeBaseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<KnowledgeBase> findByUserId(Long userId) {
        return jdbcTemplate.query(
                "SELECT id, user_id, name, description, is_delete, created_at, updated_at " +
                "FROM knowledge_base WHERE user_id = ? AND is_delete = 0 ORDER BY created_at DESC",
                (rs, rowNum) -> {
                    KnowledgeBase kb = new KnowledgeBase();
                    kb.setId(rs.getLong("id"));
                    kb.setUserId(rs.getLong("user_id"));
                    kb.setName(rs.getString("name"));
                    kb.setDescription(rs.getString("description"));
                    kb.setIsDelete(rs.getInt("is_delete") == 1);
                    kb.setCreatedAt(rs.getString("created_at"));
                    kb.setUpdatedAt(rs.getString("updated_at"));
                    return kb;
                },
                userId
        );
    }

    public Optional<KnowledgeBase> findById(Long id) {
        List<KnowledgeBase> list = jdbcTemplate.query(
                "SELECT id, user_id, name, description, is_delete, created_at, updated_at " +
                "FROM knowledge_base WHERE id = ? AND is_delete = 0 LIMIT 1",
                (rs, rowNum) -> {
                    KnowledgeBase kb = new KnowledgeBase();
                    kb.setId(rs.getLong("id"));
                    kb.setUserId(rs.getLong("user_id"));
                    kb.setName(rs.getString("name"));
                    kb.setDescription(rs.getString("description"));
                    kb.setIsDelete(rs.getInt("is_delete") == 1);
                    kb.setCreatedAt(rs.getString("created_at"));
                    kb.setUpdatedAt(rs.getString("updated_at"));
                    return kb;
                },
                id
        );
        return list.stream().findFirst();
    }

    public KnowledgeBase create(Long userId, String name, String description) {
        jdbcTemplate.update(
                "INSERT INTO knowledge_base(user_id, name, description, is_delete) VALUES (?, ?, ?, 0)",
                userId,
                name,
                description
        );
        return findByUserId(userId).get(0);
    }

    public void delete(Long id) {
        jdbcTemplate.update(
                "UPDATE knowledge_base SET is_delete = 1 WHERE id = ?",
                id
        );
    }

    public void update(Long id, String name, String description) {
        jdbcTemplate.update(
                "UPDATE knowledge_base SET name = ?, description = ? WHERE id = ?",
                name,
                description,
                id
        );
    }
}