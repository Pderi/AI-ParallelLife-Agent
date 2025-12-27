package com.qin.qaiagentproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 生成报告请求DTO
 */
@Data
@Schema(description = "生成平行人生报告请求")
public class ReportRequest {
    
    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "用户消息内容，描述当前情况或想要探索的人生选择", 
            example = "我今年25岁，程序员，工作2年，在考虑是否应该继续做技术还是转行做产品经理")
    private String message;
    
    @NotBlank(message = "会话ID不能为空")
    @Schema(description = "会话ID", example = "uuid-string")
    private String chatId;
}

