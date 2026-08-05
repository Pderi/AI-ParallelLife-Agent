package com.qin.qaiagentproject.context;

import com.qin.qaiagentproject.memory.repository.RagKnowledgeRepository;
import com.qin.qaiagentproject.memory.repository.WarmMemoryRepository;
import com.qin.qaiagentproject.memory.service.HotMemoryService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prompt 拼装顺序与空数据降级（不依赖数据库）。
 */
class PromptAssemblerTest {

    private final PromptAssembler assembler = new PromptAssembler();

    @Test
    void assemble_emptyBlocks_returnsEmpty() {
        String result = assembler.assemble("", "", "", "");
        assertTrue(result.isEmpty());
    }

    @Test
    void assemble_keepsSectionOrder() {
        String result = assembler.assemble("规则A", "温知识B", "RAG片段C", "会话D");
        int hot = result.indexOf("【热记忆");
        int warm = result.indexOf("【温记忆");
        int rag = result.indexOf("【RAG");
        int session = result.indexOf("【当前会话");
        assertTrue(hot >= 0 && warm > hot && rag > warm && session > rag);
        assertTrue(result.contains("规则A"));
        assertTrue(result.contains("温知识B"));
    }

    @Test
    void formatWarm_andRag_emptySafe() {
        assertTrue(assembler.formatWarm(List.of()).isEmpty());
        assertTrue(assembler.formatRag(List.of()).isEmpty());
        assertTrue(assembler.formatHot(new HotMemoryService.HotContext("", "{}", "")).isEmpty());

        String warm = assembler.formatWarm(List.of(
                new WarmMemoryRepository.WarmMemoryHit("职业", "偏好稳健", "规划", 4, 0.1)));
        assertFalse(warm.isEmpty());
        assertTrue(warm.contains("偏好稳健"));

        String rag = assembler.formatRag(List.of(
                new RagKnowledgeRepository.RagHit("人生规划.md", "长期主义", 0.2)));
        assertTrue(rag.contains("长期主义"));
    }
}
