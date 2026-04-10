package com.offlineqa.repository;

import com.offlineqa.model.BubbleState;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class BubbleStateRepository {

    private final JdbcTemplate jdbcTemplate;

    public BubbleStateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<BubbleState> list(Long userId, Long sessionId) {
        return jdbcTemplate.query(
                """
                SELECT pair_id, pos_x, pos_y, bubble_width, hidden
                FROM chat_bubble_state
                WHERE user_id = ? AND session_id = ?
                """,
                (rs, rowNum) -> {
                    BubbleState state = new BubbleState();
                    state.setPairId(rs.getString("pair_id"));
                    state.setX(rs.getDouble("pos_x"));
                    state.setY(rs.getDouble("pos_y"));
                    state.setWidth(rs.getDouble("bubble_width"));
                    state.setHidden(rs.getInt("hidden") == 1);
                    return state;
                },
                userId,
                sessionId
        );
    }

    public void upsert(Long userId, Long sessionId, BubbleState state) {
        jdbcTemplate.update(
                """
                INSERT INTO chat_bubble_state(user_id, session_id, pair_id, pos_x, pos_y, bubble_width, hidden, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  pos_x = VALUES(pos_x),
                  pos_y = VALUES(pos_y),
                  bubble_width = VALUES(bubble_width),
                  hidden = VALUES(hidden),
                  updated_at = VALUES(updated_at)
                """,
                userId,
                sessionId,
                state.getPairId(),
                state.getX() == null ? 0D : state.getX(),
                state.getY() == null ? 0D : state.getY(),
                state.getWidth() == null ? 560D : state.getWidth(),
                Boolean.TRUE.equals(state.getHidden()) ? 1 : 0,
                Timestamp.valueOf(LocalDateTime.now())
        );
    }
}
