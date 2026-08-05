package com.qin.qaiagentproject.context;

import lombok.Builder;
import lombok.Data;

/**
 * 三层记忆 + RAG 拼装结果。
 */
@Data
@Builder
public class ContextPackage {

    private String userId;
    private String sessionId;
    private String hotBlock;
    private String warmBlock;
    private String ragBlock;
    private String sessionBlock;
    /** 已按固定顺序拼好的增强段（不含系统人设与用户问题） */
    private String augmentation;
}
