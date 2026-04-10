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
                "SELECT id, username, nickname, password FROM sys_user WHERE username = ? LIMIT 1",
                (rs, rowNum) -> {
                    SysUser user = new SysUser();
                    user.setId(rs.getLong("id"));
                    user.setUsername(rs.getString("username"));
                    user.setNickname(rs.getString("nickname"));
                    user.setPassword(rs.getString("password"));
                    return user;
                },
                username
        );
        return list.stream().findFirst();
    }

    public Optional<SysUser> findById(long userId) {
        var list = jdbcTemplate.query(
                "SELECT id, username, nickname, password FROM sys_user WHERE id = ? LIMIT 1",
                (rs, rowNum) -> {
                    SysUser user = new SysUser();
                    user.setId(rs.getLong("id"));
                    user.setUsername(rs.getString("username"));
                    user.setNickname(rs.getString("nickname"));
                    user.setPassword(rs.getString("password"));
                    return user;
                },
                userId
        );
        return list.stream().findFirst();
    }

    public long createIfAbsent(String username) {
        System.out.println("createIfAbsent called with username: " + username);
        try {
            Optional<SysUser> existing = findByUsername(username);
            System.out.println("Existing user found: " + existing.isPresent());
            if (existing.isPresent()) {
                System.out.println("Returning existing user ID: " + existing.get().getId());
                return existing.get().getId();
            }
            System.out.println("Inserting new user: " + username);
            jdbcTemplate.update(
                    "INSERT INTO sys_user(username, nickname, password, status) VALUES (?, ?, '', 1)",
                    username,
                    username
            );
            System.out.println("Insert completed, finding user...");
            Optional<SysUser> newUser = findByUsername(username);
            System.out.println("New user found: " + newUser.isPresent());
            return newUser.orElseThrow(() -> new RuntimeException("User not found after insertion")).getId();
        } catch (Exception e) {
            System.out.println("Error in createIfAbsent: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public long register(String username, String rawPassword, String nickname) {
        Optional<SysUser> existing = findByUsername(username);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("用户名已存在");
        }
        String finalNickname = (nickname == null || nickname.isBlank()) ? username : nickname;
        jdbcTemplate.update(
                "INSERT INTO sys_user(username, nickname, password, status) VALUES (?, ?, ?, 1)",
                username,
                finalNickname,
                rawPassword
        );
        return findByUsername(username).orElseThrow().getId();
    }
}
