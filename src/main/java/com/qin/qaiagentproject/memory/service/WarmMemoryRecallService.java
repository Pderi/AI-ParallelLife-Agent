package com.qin.qaiagentproject.memory.service;

import com.qin.qaiagentproject.config.MemoryProperties;
import com.qin.qaiagentproject.memory.repository.WarmMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "memory.provider", havingValue = "pg")
@RequiredArgsConstructor
@Slf4j
public class WarmMemoryRecallService {

    private final WarmMemoryRepository warmMemoryRepository;
    private final EmbeddingService embeddingService;
    private final MemoryProperties memoryProperties;

    public List<WarmMemoryRepository.WarmMemoryHit> recall(UUID userId, String query, Integer topK) {
        int k = topK == null ? memoryProperties.getWarmTopK() : topK;
        try {
            String vectorLiteral = embeddingService.embedAsPgLiteral(query);
            return warmMemoryRepository.recallByVector(userId, vectorLiteral, k);
        } catch (Exception ex) {
            log.warn("温记忆召回失败，降级为空: userId={}, err={}", userId, ex.getMessage());
            return Collections.emptyList();
        }
    }
}
