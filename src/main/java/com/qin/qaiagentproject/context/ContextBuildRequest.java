package com.qin.qaiagentproject.context;

import lombok.Builder;
import lombok.Data;

/**
 * 上下文拼装请求。
 */
@Data
@Builder
public class ContextBuildRequest {

    private String userId;
    private String sessionId;
    private String message;
    private boolean useRag;
}
