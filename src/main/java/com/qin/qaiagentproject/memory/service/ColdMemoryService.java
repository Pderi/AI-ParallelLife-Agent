package com.qin.qaiagentproject.memory.service;

import com.qin.qaiagentproject.memory.repository.ColdMemoryRepository;
import com.qin.qaiagentproject.memory.repository.HotMemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.UUID;

@Service
@ConditionalOnProperty(name = "memory.provider", havingValue = "pg")
@RequiredArgsConstructor
public class ColdMemoryService {

    private final ColdMemoryRepository coldMemoryRepository;
    private final HotMemoryRepository hotMemoryRepository;
    private final HotMemoryService hotMemoryService;

    public void append(UUID userId, UUID sessionId, String query, String answer) {
        Assert.notNull(userId, "userId must not be null");
        Assert.notNull(sessionId, "sessionId must not be null");
        Assert.hasText(query, "query must not be blank");
        Assert.hasText(answer, "answer must not be blank");

        coldMemoryRepository.append(userId, sessionId, query, answer);
        // 同步刷新热记忆中的会话滑动窗口文本
        String sessionContext = hotMemoryService.buildRecentSessionText(userId, sessionId);
        hotMemoryRepository.updateSessionContext(userId, sessionContext);
    }

    public int countTurns(UUID userId, UUID sessionId) {
        return coldMemoryRepository.countBySession(userId, sessionId);
    }
}
