package com.qin.qaiagentproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 创建会话请求DTO
 */
@Data
@Schema(description = "创建会话请求")
public class CreateSessionRequest {
    
    @Schema(description = "用户ID（可选）", example = "user123")
    private String userId;
    
    @Schema(description = "会话名称（可选）", example = "我的职业规划")
    private String sessionName;
}

