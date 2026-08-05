package com.qin.qaiagentproject.context;

import com.qin.qaiagentproject.memory.repository.RagKnowledgeRepository;
import com.qin.qaiagentproject.memory.repository.WarmMemoryRepository;
import com.qin.qaiagentproject.memory.service.HotMemoryService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 固定顺序拼装：热 → 温 → RAG → 当前会话上下文。
 */
@Component
public class PromptAssembler {

    public String assemble(String hotBlock, String warmBlock, String ragBlock, String sessionBlock) {
        StringBuilder sb = new StringBuilder();
        appendSection(sb, "【热记忆 / 用户全局规则与索引】", hotBlock);
        appendSection(sb, "【温记忆 / 用户个性化知识】", warmBlock);
        appendSection(sb, "【RAG 知识库】", ragBlock);
        appendSection(sb, "【当前会话上下文（最近轮次）】", sessionBlock);
        return sb.toString().trim();
    }

    public String formatHot(HotMemoryService.HotContext hot) {
        if (hot == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(hot.globalRules())) {
            sb.append("全局规则：\n").append(hot.globalRules()).append("\n");
        }
        if (StringUtils.hasText(hot.globalIndexJson()) && !"{}".equals(hot.globalIndexJson().trim())) {
            sb.append("温记忆索引：").append(hot.globalIndexJson()).append("\n");
        }
        return sb.toString().trim();
    }

    public String formatWarm(List<WarmMemoryRepository.WarmMemoryHit> hits) {
        if (hits == null || hits.isEmpty()) {
            return "";
        }
        return hits.stream()
                .map(h -> "- [" + h.topicName() + "/" + h.scene() + "] " + h.content())
                .collect(Collectors.joining("\n"));
    }

    public String formatRag(List<RagKnowledgeRepository.RagHit> hits) {
        if (hits == null || hits.isEmpty()) {
            return "";
        }
        return hits.stream()
                .map(h -> "- 《" + h.docTitle() + "》" + h.docChunk())
                .collect(Collectors.joining("\n"));
    }

    private static void appendSection(StringBuilder sb, String title, String body) {
        if (!StringUtils.hasText(body)) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append("\n\n");
        }
        sb.append(title).append("\n").append(body.trim());
    }
}
