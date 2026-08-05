package com.qin.qaiagentproject.memory.service;

import com.qin.qaiagentproject.config.MemoryProperties;
import com.qin.qaiagentproject.memory.repository.RagKnowledgeRepository;
import com.qin.qaiagentproject.rag.ParallelLifeDocumentLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * pg 模式下空表时灌入领域文档向量（替代 SimpleVectorStore 演示数据）。
 */
@Component
@ConditionalOnProperty(name = "memory.provider", havingValue = "pg")
@RequiredArgsConstructor
@Slf4j
public class RagKnowledgeSeedLoader {

    private static final int MAX_CHUNKS = 40;
    private static final int MAX_CHUNK_CHARS = 800;

    private final RagKnowledgeRepository ragKnowledgeRepository;
    private final EmbeddingService embeddingService;
    private final ParallelLifeDocumentLoader parallelLifeDocumentLoader;
    private final MemoryProperties memoryProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void seedIfEmpty() {
        if (!memoryProperties.isSeedRagOnStartup()) {
            return;
        }
        if (ragKnowledgeRepository.countValid() > 0) {
            log.info("rag_knowledge 已有数据，跳过种子灌入");
            return;
        }

        List<Document> documents = parallelLifeDocumentLoader.loadMarkdowns();
        int inserted = 0;
        for (Document document : documents) {
            if (inserted >= MAX_CHUNKS) {
                break;
            }
            String text = document.getText();
            if (text == null || text.isBlank()) {
                continue;
            }
            String chunk = text.length() > MAX_CHUNK_CHARS
                    ? text.substring(0, MAX_CHUNK_CHARS)
                    : text;
            String title = String.valueOf(document.getMetadata()
                    .getOrDefault("filename", "untitled"));
            try {
                String vectorLiteral = embeddingService.embedAsPgLiteral(chunk);
                ragKnowledgeRepository.insert(title, chunk, vectorLiteral);
                inserted++;
            } catch (Exception ex) {
                log.warn("RAG 种子写入失败: title={}, err={}", title, ex.getMessage());
            }
        }
        log.info("RAG 种子灌入完成: inserted={}", inserted);
    }
}
