package com.qin.qaiagentproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会话信息")
public class SessionResponse {
    
    @Schema(description = "会话ID")
    private String chatId;
    
    @Schema(description = "用户ID")
    private String userId;
    
    @Schema(description = "会话名称")
    private String sessionName;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "最后更新时间")
    private LocalDateTime updateTime;
}

