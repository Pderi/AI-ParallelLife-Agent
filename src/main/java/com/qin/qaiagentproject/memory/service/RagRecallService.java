package com.qin.qaiagentproject.memory.service;

import com.qin.qaiagentproject.config.MemoryProperties;
import com.qin.qaiagentproject.memory.repository.RagKnowledgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@ConditionalOnProperty(name = "memory.provider", havingValue = "pg")
@RequiredArgsConstructor
@Slf4j
public class RagRecallService {

    private final RagKnowledgeRepository ragKnowledgeRepository;
    private final EmbeddingService embeddingService;
    private final MemoryProperties memoryProperties;

    public List<RagKnowledgeRepository.RagHit> recall(String query, Integer topK) {
        int k = topK == null ? memoryProperties.getRagTopK() : topK;
        try {
            String vectorLiteral = embeddingService.embedAsPgLiteral(query);
            return ragKnowledgeRepository.recallByVector(vectorLiteral, k);
        } catch (Exception ex) {
            log.warn("RAG 召回失败，降级为空: err={}", ex.getMessage());
            return Collections.emptyList();
        }
    }
}
