package com.offlineqa.repository;

import com.offlineqa.model.SysUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<SysUser> findByUsername(String username) {
        var list = jdbcTemplate.query(
                "SELECT id, username, nickname FROM sys_user WHERE username = ? LIMIT 1",
                (rs, rowNum) -> {
                    SysUser user = new SysUser();
                    user.setId(rs.getLong("id"));
                    user.setUsername(rs.getString("username"));
                    user.setNickname(rs.getString("nickname"));
                    return user;
                },
                username
        );
        return list.stream().findFirst();
    }

    public long createIfAbsent(String username) {
        Optional<SysUser> existing = findByUsername(username);
        if (existing.isPresent()) {
            return existing.get().getId();
        }
        jdbcTemplate.update(
                "INSERT INTO sys_user(username, nickname, password, status) VALUES (?, ?, '', 1)",
                username,
                username
        );
        return findByUsername(username).orElseThrow().getId();
    }
}
