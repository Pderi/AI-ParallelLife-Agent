package com.qin.qaiagentproject.memory.service;

import com.qin.qaiagentproject.config.MemoryProperties;
import com.qin.qaiagentproject.memory.repository.ColdMemoryRepository;
import com.qin.qaiagentproject.memory.repository.HotMemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "memory.provider", havingValue = "pg")
@RequiredArgsConstructor
public class HotMemoryService {

    private final HotMemoryRepository hotMemoryRepository;
    private final ColdMemoryRepository coldMemoryRepository;
    private final MemoryProperties memoryProperties;

    public HotContext getHot(UUID userId, UUID sessionId) {
        HotMemoryRepository.HotMemoryRow row = hotMemoryRepository.findByUserId(userId)
                .orElse(new HotMemoryRepository.HotMemoryRow("", "", "{}"));
        String recent = buildRecentSessionText(userId, sessionId);
        return new HotContext(
                nullToEmpty(row.globalRules()),
                nullToEmpty(row.globalIndexJson()),
                recent);
    }

    public String buildRecentSessionText(UUID userId, UUID sessionId) {
        List<ColdMemoryRepository.ColdMemoryRow> rounds = coldMemoryRepository.findRecentRounds(
                userId, sessionId, memoryProperties.getColdRecentRounds());
        if (rounds.isEmpty()) {
            return "";
        }
        return rounds.stream()
                .map(r -> "用户：" + r.query() + "\n助手：" + r.answer())
                .collect(Collectors.joining("\n\n"));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public record HotContext(String globalRules, String globalIndexJson, String sessionContext) {
    }
}
