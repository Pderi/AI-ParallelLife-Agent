package com.qin.qaiagentproject.memory.repository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "memory.provider", havingValue = "pg")
public class WarmMemoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public WarmMemoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(UUID userId, String topicName, String content, String scene,
                       int importance, String vectorLiteral) {
        jdbcTemplate.update(
                """
                INSERT INTO warm_memory (user_id, topic_name, content, scene, importance, content_vector)
                VALUES (?, ?, ?, ?, ?, ?::vector)
                ON CONFLICT (user_id, topic_name) DO UPDATE
                SET content = EXCLUDED.content,
                    scene = EXCLUDED.scene,
                    importance = EXCLUDED.importance,
                    content_vector = EXCLUDED.content_vector,
                    updated_at = NOW(),
                    is_archived = false
                """,
                userId, topicName, content, scene, importance, vectorLiteral);
    }

    public List<WarmMemoryHit> recallByVector(UUID userId, String vectorLiteral, int topK) {
        return jdbcTemplate.query(
                """
                SELECT topic_name, content, scene, importance,
                       (content_vector <=> ?::vector) AS distance
                FROM warm_memory
                WHERE user_id = ? AND is_archived = false
                ORDER BY content_vector <=> ?::vector
                LIMIT ?
                """,
                (rs, rowNum) -> new WarmMemoryHit(
                        rs.getString("topic_name"),
                        rs.getString("content"),
                        rs.getString("scene"),
                        rs.getInt("importance"),
                        rs.getDouble("distance")),
                vectorLiteral, userId, vectorLiteral, topK);
    }

    public record WarmMemoryHit(String topicName, String content, String scene,
                                int importance, double distance) {
    }
}
