package com.qin.qaiagentproject.memory.repository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "memory.provider", havingValue = "pg")
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsById(UUID userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM users WHERE user_id = ?",
                Integer.class,
                userId);
        return count != null && count > 0;
    }

    public Optional<UUID> findIdByUsername(String username) {
        return jdbcTemplate.query(
                "SELECT user_id FROM users WHERE username = ?",
                rs -> rs.next() ? Optional.of((UUID) rs.getObject("user_id")) : Optional.empty(),
                username);
    }

    public void insert(UUID userId, String username) {
        jdbcTemplate.update(
                """
                INSERT INTO users (user_id, username)
                VALUES (?, ?)
                ON CONFLICT (user_id) DO NOTHING
                """,
                userId, username);
    }

    public void touchLastActive(UUID userId) {
        jdbcTemplate.update(
                "UPDATE users SET last_active_at = NOW() WHERE user_id = ?",
                userId);
    }
}
