package com.qin.qaiagentproject.context;

import com.qin.qaiagentproject.config.MemoryProperties;
import com.qin.qaiagentproject.exception.BusinessException;
import com.qin.qaiagentproject.exception.ErrorCode;
import com.qin.qaiagentproject.memory.repository.ColdMemoryRepository;
import com.qin.qaiagentproject.memory.service.ColdMemoryService;
import com.qin.qaiagentproject.memory.service.HotMemoryService;
import com.qin.qaiagentproject.memory.service.MemoryExtractService;
import com.qin.qaiagentproject.memory.service.RagRecallService;
import com.qin.qaiagentproject.memory.service.UserMemoryBootstrapService;
import com.qin.qaiagentproject.memory.service.WarmMemoryRecallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Memory Manager MVP：并行召回热/温/RAG，afterRun 写冷并异步提炼。
 */
@Service
@ConditionalOnProperty(name = "memory.provider", havingValue = "pg")
@RequiredArgsConstructor
@Slf4j
public class ContextManager {

    private final UserMemoryBootstrapService userMemoryBootstrapService;
    private final HotMemoryService hotMemoryService;
    private final WarmMemoryRecallService warmMemoryRecallService;
    private final RagRecallService ragRecallService;
    private final ColdMemoryService coldMemoryService;
    private final MemoryExtractService memoryExtractService;
    private final PromptAssembler promptAssembler;
    private final MemoryProperties memoryProperties;
    private final ColdMemoryRepository coldMemoryRepository;

    private final ExecutorService recallExecutor = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "context-recall");
        t.setDaemon(true);
        return t;
    });

    public ContextPackage buildContext(ContextBuildRequest request) {
        if (request == null || !StringUtils.hasText(request.getMessage())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "上下文构建请求无效");
        }
        if (!StringUtils.hasText(request.getSessionId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "sessionId 不能为空");
        }

        UUID sessionId = parseUuid(request.getSessionId(), "sessionId");
        UUID userId;
        if (StringUtils.hasText(request.getUserId())) {
            userId = userMemoryBootstrapService.ensureUser(request.getUserId());
        } else {
            userId = coldMemoryRepository.findUserIdBySession(sessionId)
                    .orElseGet(() -> userMemoryBootstrapService.ensureUser(null));
        }

        CompletableFuture<HotMemoryService.HotContext> hotFuture = CompletableFuture.supplyAsync(
                () -> hotMemoryService.getHot(userId, sessionId), recallExecutor);

        CompletableFuture<java.util.List<com.qin.qaiagentproject.memory.repository.WarmMemoryRepository.WarmMemoryHit>> warmFuture =
                CompletableFuture.supplyAsync(
                        () -> warmMemoryRecallService.recall(userId, request.getMessage(), memoryProperties.getWarmTopK()),
                        recallExecutor);

        CompletableFuture<java.util.List<com.qin.qaiagentproject.memory.repository.RagKnowledgeRepository.RagHit>> ragFuture =
                request.isUseRag()
                        ? CompletableFuture.supplyAsync(
                        () -> ragRecallService.recall(request.getMessage(), memoryProperties.getRagTopK()),
                        recallExecutor)
                        : CompletableFuture.completedFuture(Collections.emptyList());

        HotMemoryService.HotContext hot = hotFuture.join();
        var warmHits = warmFuture.join();
        var ragHits = ragFuture.join();

        String hotBlock = promptAssembler.formatHot(hot);
        String warmBlock = promptAssembler.formatWarm(warmHits);
        String ragBlock = promptAssembler.formatRag(ragHits);
        String sessionBlock = hot == null ? "" : nullToEmpty(hot.sessionContext());
        String augmentation = promptAssembler.assemble(hotBlock, warmBlock, ragBlock, sessionBlock);

        return ContextPackage.builder()
                .userId(userId.toString())
                .sessionId(sessionId.toString())
                .hotBlock(hotBlock)
                .warmBlock(warmBlock)
                .ragBlock(ragBlock)
                .sessionBlock(sessionBlock)
                .augmentation(augmentation)
                .build();
    }

    /**
     * 先写冷记忆，再按阈值触发异步提炼。
     */
    public void afterRun(String userId, String sessionId, String query, String answer) {
        UUID uid = parseUuid(userId, "userId");
        UUID sid = parseUuid(sessionId, "sessionId");
        coldMemoryService.append(uid, sid, query, answer);

        int turns = coldMemoryService.countTurns(uid, sid);
        if (turns >= memoryProperties.getExtractMinTurns()) {
            memoryExtractService.extractColdToWarmAsync(uid, sid);
        }
    }

    private static UUID parseUuid(String value, String field) {
        try {
            return UUID.fromString(value.trim());
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, field + " 必须是合法 UUID");
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
