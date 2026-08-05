package com.qin.qaiagentproject.controller;

import com.qin.qaiagentproject.app.ParallelLifeApp;
import com.qin.qaiagentproject.common.Result;
import com.qin.qaiagentproject.config.MemoryProperties;
import com.qin.qaiagentproject.dto.ChatRequest;
import com.qin.qaiagentproject.dto.ChatResponse;
import com.qin.qaiagentproject.dto.CreateSessionRequest;
import com.qin.qaiagentproject.dto.ReportRequest;
import com.qin.qaiagentproject.dto.SessionResponse;
import com.qin.qaiagentproject.memory.service.UserMemoryBootstrapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 平行宇宙人生模拟器 API 控制器
 */
@RestController
@RequestMapping("/parallel-life")
@Tag(name = "平行宇宙人生模拟器", description = "通过AI模拟不同人生路径，帮助用户探索人生可能性")
@Slf4j
public class ParallelLifeController {

    @Resource
    private ParallelLifeApp parallelLifeApp;

    @Resource
    private MemoryProperties memoryProperties;

    @Resource
    private ObjectProvider<UserMemoryBootstrapService> userMemoryBootstrapServiceProvider;

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式对话", description = "与AI进行流式对话，实时返回AI回复内容")
    public Flux<String> chatStream(@Valid @RequestBody ChatRequest request) {
        log.info("收到流式对话请求: chatId={}, userId={}, useRag={}, useTools={}",
                request.getChatId(), request.getUserId(), request.getUseRag(), request.getUseTools());

        if (Boolean.TRUE.equals(request.getUseTools())) {
            return parallelLifeApp.doChatWithToolsStream(request.getMessage(), request.getChatId());
        }
        return parallelLifeApp.doChatStream(
                request.getMessage(),
                request.getChatId(),
                request.getUserId(),
                Boolean.TRUE.equals(request.getUseRag()));
    }

    @PostMapping("/chat")
    @Operation(summary = "基础对话（非流式）", description = "与AI进行对话，等待完整回复后返回（推荐使用流式接口）")
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("收到对话请求: chatId={}, userId={}, useRag={}, useTools={}",
                request.getChatId(), request.getUserId(), request.getUseRag(), request.getUseTools());

        String content;
        if (Boolean.TRUE.equals(request.getUseTools())) {
            content = parallelLifeApp.doChatWithTools(request.getMessage(), request.getChatId());
        } else {
            content = parallelLifeApp.doChat(
                    request.getMessage(),
                    request.getChatId(),
                    request.getUserId(),
                    Boolean.TRUE.equals(request.getUseRag()));
        }

        ChatResponse response = new ChatResponse(content, request.getChatId());
        return Result.success(response);
    }

    @PostMapping("/report")
    @Operation(summary = "生成平行人生报告", description = "生成结构化的平行人生报告，包含多个平行宇宙的详细分析")
    public Result<ParallelLifeApp.ParallelLifeReport> generateReport(@Valid @RequestBody ReportRequest request) {
        log.info("收到报告生成请求: chatId={}, message={}", request.getChatId(), request.getMessage());

        ParallelLifeApp.ParallelLifeReport report =
                parallelLifeApp.doChatWithReport(request.getMessage(), request.getChatId());

        return Result.success("报告生成成功", report);
    }

    @PostMapping("/session")
    @Operation(summary = "创建会话", description = "创建一个新的对话会话，返回会话ID；pg 模式下会初始化 users + hot_memory")
    public Result<SessionResponse> createSession(@RequestBody(required = false) CreateSessionRequest request) {
        String chatId = UUID.randomUUID().toString();
        String userId = request != null ? request.getUserId() : null;
        String sessionName = request != null ? request.getSessionName() : null;

        if (memoryProperties.isPgProvider()) {
            UserMemoryBootstrapService bootstrap = userMemoryBootstrapServiceProvider.getIfAvailable();
            if (bootstrap != null) {
                userId = bootstrap.ensureUser(userId).toString();
            }
        } else if (!StringUtils.hasText(userId)) {
            userId = null;
        }

        SessionResponse response = new SessionResponse(
                chatId,
                userId,
                sessionName,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        log.info("创建新会话: chatId={}, userId={}, sessionName={}, provider={}",
                chatId, userId, sessionName, memoryProperties.getProvider());
        return Result.success("会话创建成功", response);
    }

    @PostMapping(value = "/quick-chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "快速流式对话", description = "快速流式对话接口，无需提供chatId，系统自动创建会话")
    public Flux<String> quickChatStream(
            @RequestBody @Parameter(description = "用户消息") String message,
            @RequestParam(required = false) @Parameter(description = "会话ID（可选，如果提供则使用现有会话）") String chatId) {
        if (message == null || message.trim().isEmpty()) {
            return Flux.error(new IllegalArgumentException("消息内容不能为空"));
        }

        if (chatId == null || chatId.trim().isEmpty()) {
            chatId = UUID.randomUUID().toString();
            log.info("快速流式对话（新会话）: chatId={}, message={}", chatId, message);
        } else {
            log.info("快速流式对话（现有会话）: chatId={}, message={}", chatId, message);
        }

        return parallelLifeApp.doChatStream(message, chatId);
    }

    @PostMapping("/quick-chat")
    @Operation(summary = "快速对话（非流式）", description = "快速对话接口，无需提供chatId，系统自动创建会话（推荐使用流式接口）")
    public Result<ChatResponse> quickChat(@RequestBody @Parameter(description = "用户消息") String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }

        String chatId = UUID.randomUUID().toString();
        log.info("快速对话: chatId={}, message={}", chatId, message);

        String content = parallelLifeApp.doChat(message, chatId);
        ChatResponse response = new ChatResponse(content, chatId);

        return Result.success(response);
    }

    @GetMapping("/session/{chatId}")
    @Operation(summary = "获取会话信息", description = "根据会话ID获取会话信息")
    public Result<SessionResponse> getSession(
            @PathVariable @Parameter(description = "会话ID") String chatId) {
        SessionResponse response = new SessionResponse(
                chatId,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        return Result.success(response);
    }
}
