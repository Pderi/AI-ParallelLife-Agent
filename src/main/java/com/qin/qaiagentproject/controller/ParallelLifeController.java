package com.qin.qaiagentproject.controller;

import com.qin.qaiagentproject.app.ParallelLifeApp;
import com.qin.qaiagentproject.common.Result;
import com.qin.qaiagentproject.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
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

    /**
     * 流式对话接口（推荐使用）
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式对话", description = "与AI进行流式对话，实时返回AI回复内容")
    public Flux<String> chatStream(@Valid @RequestBody ChatRequest request) {
        log.info("收到流式对话请求: chatId={}, message={}, useRag={}, useTools={}", 
                request.getChatId(), request.getMessage(), request.getUseRag(), request.getUseTools());
        
        if (Boolean.TRUE.equals(request.getUseTools())) {
            return parallelLifeApp.doChatWithToolsStream(request.getMessage(), request.getChatId());
        } else if (Boolean.TRUE.equals(request.getUseRag())) {
            return parallelLifeApp.doChatWithRagStream(request.getMessage(), request.getChatId());
        } else {
            return parallelLifeApp.doChatStream(request.getMessage(), request.getChatId());
        }
    }

    /**
     * 基础对话接口（非流式，兼容旧版本）
     */
    @PostMapping("/chat")
    @Operation(summary = "基础对话（非流式）", description = "与AI进行对话，等待完整回复后返回（推荐使用流式接口）")
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("收到对话请求: chatId={}, message={}, useRag={}, useTools={}", 
                request.getChatId(), request.getMessage(), request.getUseRag(), request.getUseTools());
        
        String content;
        if (Boolean.TRUE.equals(request.getUseTools())) {
            content = parallelLifeApp.doChatWithTools(request.getMessage(), request.getChatId());
        } else if (Boolean.TRUE.equals(request.getUseRag())) {
            content = parallelLifeApp.doChatWithRag(request.getMessage(), request.getChatId());
        } else {
            content = parallelLifeApp.doChat(request.getMessage(), request.getChatId());
        }
        
        ChatResponse response = new ChatResponse(content, request.getChatId());
        return Result.success(response);
    }

    /**
     * 生成平行人生报告
     */
    @PostMapping("/report")
    @Operation(summary = "生成平行人生报告", description = "生成结构化的平行人生报告，包含多个平行宇宙的详细分析")
    public Result<ParallelLifeApp.ParallelLifeReport> generateReport(@Valid @RequestBody ReportRequest request) {
        log.info("收到报告生成请求: chatId={}, message={}", request.getChatId(), request.getMessage());
        
        ParallelLifeApp.ParallelLifeReport report = 
                parallelLifeApp.doChatWithReport(request.getMessage(), request.getChatId());
        
        return Result.success("报告生成成功", report);
    }

    /**
     * 创建新会话
     */
    @PostMapping("/session")
    @Operation(summary = "创建会话", description = "创建一个新的对话会话，返回会话ID")
    public Result<SessionResponse> createSession(@RequestBody(required = false) CreateSessionRequest request) {
        String chatId = UUID.randomUUID().toString();
        String userId = request != null ? request.getUserId() : null;
        String sessionName = request != null ? request.getSessionName() : null;
        
        SessionResponse response = new SessionResponse(
                chatId,
                userId,
                sessionName,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        
        log.info("创建新会话: chatId={}, userId={}, sessionName={}", chatId, userId, sessionName);
        return Result.success("会话创建成功", response);
    }

    /**
     * 快速流式对话（简化版，自动创建会话）
     */
    @PostMapping(value = "/quick-chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "快速流式对话", description = "快速流式对话接口，无需提供chatId，系统自动创建会话")
    public Flux<String> quickChatStream(
            @RequestBody @Parameter(description = "用户消息") String message,
            @RequestParam(required = false) @Parameter(description = "会话ID（可选，如果提供则使用现有会话）") String chatId) {
        if (message == null || message.trim().isEmpty()) {
            return Flux.error(new IllegalArgumentException("消息内容不能为空"));
        }
        
        // 如果没有提供chatId，自动创建新会话
        if (chatId == null || chatId.trim().isEmpty()) {
            chatId = UUID.randomUUID().toString();
            log.info("快速流式对话（新会话）: chatId={}, message={}", chatId, message);
        } else {
            log.info("快速流式对话（现有会话）: chatId={}, message={}", chatId, message);
        }
        
        return parallelLifeApp.doChatStream(message, chatId);
    }

    /**
     * 快速对话（简化版，自动创建会话，非流式）
     */
    @PostMapping("/quick-chat")
    @Operation(summary = "快速对话（非流式）", description = "快速对话接口，无需提供chatId，系统自动创建会话（推荐使用流式接口）")
    public Result<ChatResponse> quickChat(@RequestBody @Parameter(description = "用户消息") String message) {
        if (message == null || message.trim().isEmpty()) {
            return Result.error(400, "消息内容不能为空");
        }
        
        // 自动创建会话
        String chatId = UUID.randomUUID().toString();
        log.info("快速对话: chatId={}, message={}", chatId, message);
        
        String content = parallelLifeApp.doChat(message, chatId);
        ChatResponse response = new ChatResponse(content, chatId);
        
        return Result.success(response);
    }

    /**
     * 获取会话信息（占位接口，实际需要实现会话存储）
     */
    @GetMapping("/session/{chatId}")
    @Operation(summary = "获取会话信息", description = "根据会话ID获取会话信息")
    public Result<SessionResponse> getSession(
            @PathVariable @Parameter(description = "会话ID") String chatId) {
        // TODO: 实际应该从存储中获取会话信息
        // 这里返回一个示例响应
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

