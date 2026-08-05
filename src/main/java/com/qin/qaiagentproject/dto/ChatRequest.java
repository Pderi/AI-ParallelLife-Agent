package com.qin.qaiagentproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 对话请求DTO
 */
@Data
@Schema(description = "对话请求")
public class ChatRequest {
    
    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "用户消息内容", example = "我今年25岁，是一名程序员，工作2年了")
    private String message;
    
    @NotBlank(message = "会话ID不能为空")
    @Schema(description = "会话ID，用于维护对话上下文", example = "uuid-string")
    private String chatId;

    @Schema(description = "用户ID（可选；pg 模式下缺省由后端创建匿名用户）", example = "550e8400-e29b-41d4-a716-446655440000")
    private String userId;
    
    @Schema(description = "是否使用RAG增强（结合知识库）", example = "false")
    private Boolean useRag = false;
    
    @Schema(description = "是否使用工具调用", example = "false")
    private Boolean useTools = false;
}

