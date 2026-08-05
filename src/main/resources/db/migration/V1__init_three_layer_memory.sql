-- 三层记忆 MVP DDL（对齐 docs-back/02-架构与方案/简化实现方案.md 第四节）
-- 向量维度 1536：与 DashScope text-embedding-v2 / 配置约定一致

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_active_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE hot_memory (
    user_id UUID PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    global_rules TEXT NOT NULL DEFAULT '',
    session_context TEXT NOT NULL DEFAULT '',
    global_index JSONB NOT NULL DEFAULT '{}'
);

CREATE TABLE warm_memory (
    topic_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    topic_name VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    scene VARCHAR(100) NOT NULL,
    importance INT NOT NULL DEFAULT 3,
    content_vector vector(1536) NOT NULL,
    is_archived BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, topic_name)
);

CREATE INDEX idx_warm_vector ON warm_memory USING hnsw (content_vector vector_cosine_ops);

CREATE TABLE cold_memory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    session_id UUID NOT NULL,
    query TEXT NOT NULL,
    answer TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cold_session ON cold_memory (user_id, session_id);
CREATE INDEX idx_cold_session_time ON cold_memory (user_id, session_id, created_at DESC);

CREATE TABLE rag_knowledge (
    doc_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doc_title VARCHAR(200) NOT NULL,
    doc_chunk TEXT NOT NULL,
    doc_vector vector(1536) NOT NULL,
    is_valid BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rag_vector ON rag_knowledge USING hnsw (doc_vector vector_cosine_ops);
