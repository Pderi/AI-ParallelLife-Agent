package com.qin.qaiagentproject.memory.repository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnProperty(name = "memory.provider", havingValue = "pg")
public class RagKnowledgeRepository {

    private final JdbcTemplate jdbcTemplate;

    public RagKnowledgeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int countValid() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM rag_knowledge WHERE is_valid = true",
                Integer.class);
        return count == null ? 0 : count;
    }

    public void insert(String docTitle, String docChunk, String vectorLiteral) {
        jdbcTemplate.update(
                """
                INSERT INTO rag_knowledge (doc_title, doc_chunk, doc_vector, is_valid)
                VALUES (?, ?, ?::vector, true)
                """,
                docTitle, docChunk, vectorLiteral);
    }

    public List<RagHit> recallByVector(String vectorLiteral, int topK) {
        return jdbcTemplate.query(
                """
                SELECT doc_title, doc_chunk,
                       (doc_vector <=> ?::vector) AS distance
                FROM rag_knowledge
                WHERE is_valid = true
                ORDER BY doc_vector <=> ?::vector
                LIMIT ?
                """,
                (rs, rowNum) -> new RagHit(
                        rs.getString("doc_title"),
                        rs.getString("doc_chunk"),
                        rs.getDouble("distance")),
                vectorLiteral, vectorLiteral, topK);
    }

    public record RagHit(String docTitle, String docChunk, double distance) {
    }
}
