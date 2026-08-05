package com.qin.qaiagentproject.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 三层记忆 MVP 配置。
 */
@Data
@ConfigurationProperties(prefix = "memory")
public class MemoryProperties {

    /**
     * pg：PostgreSQL 三层记忆；legacy：FileBasedChatMemory(kryo)
     */
    private String provider = "legacy";

    private int warmTopK = 3;

    private int ragTopK = 3;

    private int coldRecentRounds = 3;

    /** 会话冷记忆条数达到该值后才异步提炼温记忆 */
    private int extractMinTurns = 2;

    /** 与 Flyway DDL vector(N) 保持一致 */
    private int embeddingDimensions = 1536;

    /** pg 模式下启动时是否将 classpath:document/*.md 灌入 rag_knowledge（空表时） */
    private boolean seedRagOnStartup = true;

    public boolean isPgProvider() {
        return "pg".equalsIgnoreCase(provider);
    }

    public boolean isLegacyProvider() {
        return !isPgProvider();
    }
}
