package com.qin.qaiagentproject.memory.repository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "memory.provider", havingValue = "pg")
public class ColdMemoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public ColdMemoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UUID> findUserIdBySession(UUID sessionId) {
        return jdbcTemplate.query(
                """
                SELECT user_id FROM cold_memory
                WHERE session_id = ?
                ORDER BY created_at ASC
                LIMIT 1
                """,
                rs -> rs.next() ? Optional.of((UUID) rs.getObject("user_id")) : Optional.empty(),
                sessionId);
    }

    public void append(UUID userId, UUID sessionId, String query, String answer) {
        jdbcTemplate.update(
                """
                INSERT INTO cold_memory (user_id, session_id, query, answer)
                VALUES (?, ?, ?, ?)
                """,
                userId, sessionId, query, answer);
    }

    public int countBySession(UUID userId, UUID sessionId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM cold_memory WHERE user_id = ? AND session_id = ?",
                Integer.class,
                userId, sessionId);
        return count == null ? 0 : count;
    }

    /**
     * 最近 N 轮（按时间倒序取，再正序返回，便于拼装对话）。
     */
    public List<ColdMemoryRow> findRecentRounds(UUID userId, UUID sessionId, int limit) {
        List<ColdMemoryRow> desc = jdbcTemplate.query(
                """
                SELECT query, answer, created_at
                FROM cold_memory
                WHERE user_id = ? AND session_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new ColdMemoryRow(
                        rs.getString("query"),
                        rs.getString("answer"),
                        rs.getTimestamp("created_at").toInstant()),
                userId, sessionId, limit);
        List<ColdMemoryRow> chronological = new ArrayList<>(desc);
        Collections.reverse(chronological);
        return chronological;
    }

    public List<ColdMemoryRow> findAllBySession(UUID userId, UUID sessionId) {
        return jdbcTemplate.query(
                """
                SELECT query, answer, created_at
                FROM cold_memory
                WHERE user_id = ? AND session_id = ?
                ORDER BY created_at ASC
                """,
                (rs, rowNum) -> new ColdMemoryRow(
                        rs.getString("query"),
                        rs.getString("answer"),
                        rs.getTimestamp("created_at").toInstant()),
                userId, sessionId);
    }

    public record ColdMemoryRow(String query, String answer, Instant createdAt) {
    }
}
