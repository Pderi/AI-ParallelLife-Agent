package com.qin.qaiagentproject.memory.repository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "memory.provider", havingValue = "pg")
public class HotMemoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public HotMemoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertIfAbsent(UUID userId) {
        jdbcTemplate.update(
                """
                INSERT INTO hot_memory (user_id, global_rules, session_context, global_index)
                VALUES (?, '', '', '{}'::jsonb)
                ON CONFLICT (user_id) DO NOTHING
                """,
                userId);
    }

    public Optional<HotMemoryRow> findByUserId(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT global_rules, session_context, global_index::text AS global_index
                FROM hot_memory
                WHERE user_id = ?
                """,
                rs -> {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new HotMemoryRow(
                            rs.getString("global_rules"),
                            rs.getString("session_context"),
                            rs.getString("global_index")));
                },
                userId);
    }

    public void updateSessionContext(UUID userId, String sessionContext) {
        jdbcTemplate.update(
                "UPDATE hot_memory SET session_context = ? WHERE user_id = ?",
                sessionContext, userId);
    }

    /**
     * 将 topic -> scene 合并进 global_index（安全，避免 topic 名注入）。
     */
    public void mergeGlobalIndex(UUID userId, String topicName, String scene) {
        jdbcTemplate.update(
                """
                UPDATE hot_memory
                SET global_index = COALESCE(global_index, '{}'::jsonb) || jsonb_build_object(?, to_jsonb(?::text))
                WHERE user_id = ?
                """,
                topicName, scene, userId);
    }

    public record HotMemoryRow(String globalRules, String sessionContext, String globalIndexJson) {
    }
}
