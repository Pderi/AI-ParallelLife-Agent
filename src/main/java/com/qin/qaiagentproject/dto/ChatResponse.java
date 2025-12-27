package com.qin.qaiagentproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 对话响应DTO
 */
@Data
@Schema(description = "对话响应")
public class ChatResponse {
    
    @Schema(description = "AI回复内容")
    private String content;
    
    @Schema(description = "会话ID")
    private String chatId;
    
    public ChatResponse(String content, String chatId) {
        this.content = content;
        this.chatId = chatId;
    }
}

