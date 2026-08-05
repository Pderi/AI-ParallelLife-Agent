package com.qin.qaiagentproject.memory.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.qin.qaiagentproject.config.MemoryProperties;
import com.qin.qaiagentproject.memory.repository.ColdMemoryRepository;
import com.qin.qaiagentproject.memory.repository.HotMemoryRepository;
import com.qin.qaiagentproject.memory.repository.WarmMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 异步冷→温→热提炼（失败仅打日志，不影响主链路）。
 */
@Service
@ConditionalOnProperty(name = "memory.provider", havingValue = "pg")
@RequiredArgsConstructor
@Slf4j
public class MemoryExtractService {

    private final ColdMemoryRepository coldMemoryRepository;
    private final WarmMemoryRepository warmMemoryRepository;
    private final HotMemoryRepository hotMemoryRepository;
    private final EmbeddingService embeddingService;
    private final ChatModel chatModel;
    private final MemoryProperties memoryProperties;

    @Async("memoryExtractExecutor")
    public void extractColdToWarmAsync(UUID userId, UUID sessionId) {
        try {
            extractColdToWarm(userId, sessionId);
        } catch (Exception ex) {
            log.warn("温记忆提炼失败（已忽略）: userId={}, sessionId={}, err={}",
                    userId, sessionId, ex.getMessage(), ex);
        }
    }

    public void extractColdToWarm(UUID userId, UUID sessionId) {
        List<ColdMemoryRepository.ColdMemoryRow> rows =
                coldMemoryRepository.findAllBySession(userId, sessionId);
        if (rows.size() < memoryProperties.getExtractMinTurns()) {
            return;
        }

        String rawText = rows.stream()
                .map(r -> "用户：" + r.query() + "\nAI：" + r.answer())
                .collect(Collectors.joining("\n"));

        String extractPrompt = """
                从以下对话中提取用户的个性化规则、偏好、踩坑记录，只保留可长期复用的内容。
                要求：
                1. 只提取用户明确说的内容，不要添加通用知识
                2. 输出JSON格式，没有有效内容就输出{"has_memory": false}
                3. 每条规则不超过50字
                
                【输出格式】
                {
                  "has_memory": true,
                  "topic_name": "场景名称",
                  "scene": "生效场景",
                  "content": "提炼的知识内容（Markdown列表）",
                  "importance": 1-5
                }
                
                【对话内容】
                %s
                """.formatted(rawText);

        String content = chatModel.call(new Prompt(extractPrompt))
                .getResult()
                .getOutput()
                .getText();
        if (!StringUtils.hasText(content)) {
            return;
        }

        String json = extractJsonObject(content);
        JSONObject result = JSONUtil.parseObj(json);
        if (!result.getBool("has_memory", false)) {
            return;
        }

        String topicName = result.getStr("topic_name");
        String scene = result.getStr("scene");
        String knowledge = result.getStr("content");
        int importance = result.getInt("importance", 3);
        if (!StringUtils.hasText(topicName) || !StringUtils.hasText(knowledge)) {
            return;
        }
        if (!StringUtils.hasText(scene)) {
            scene = "通用";
        }

        String vectorLiteral = embeddingService.embedAsPgLiteral(knowledge);
        warmMemoryRepository.upsert(userId, topicName, knowledge, scene, importance, vectorLiteral);
        hotMemoryRepository.mergeGlobalIndex(userId, topicName, scene);
        log.info("温记忆提炼成功: userId={}, topic={}", userId, topicName);
    }

    private static String extractJsonObject(String text) {
        String trimmed = text.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }
}
