# 三层记忆 DDL 设计文档（Redis + PostgreSQL + MySQL）

## 1. 文档目标
本文件提供三层记忆首期落地的可执行 DDL 与键规范，配套 `三层记忆终极落地方案-项目实战版.md`。

覆盖范围：
1. PostgreSQL（温记忆）
2. MySQL（用户、会话、消息、工具审计、冷记忆）
3. Redis（热记忆 key/TTL/容量策略）

---

## 2. 全局约定
1. 主业务隔离键：`user_id`（全链路必填）。
2. 会话键：`chat_id`（外部会话 ID，字符串）。
3. 消息键：`message_id`（业务唯一 ID，幂等写入）。
4. 时间字段：
   - PostgreSQL：`timestamptz`
   - MySQL：`datetime`
5. 字符集（MySQL）：`utf8mb4`

---

## 3. PostgreSQL DDL（温记忆）

```sql
-- 建议：独立 schema
create schema if not exists agent_core;

-- 3.1 温记忆索引表
create table if not exists agent_core.memory_topic_index (
  id bigserial primary key,
  topic_id varchar(64) not null unique,
  user_id varchar(64) not null,
  chat_id varchar(64), -- null 表示全局温记忆；有值表示会话专属
  title varchar(128) not null,
  tags jsonb,
  priority varchar(16) not null default 'medium', -- high/medium/low
  quality_score numeric(4,3),
  ttl_days integer,
  last_verified_at timestamptz,
  is_archived boolean not null default false,
  updated_at timestamptz not null default now()
);

create index if not exists idx_memory_topic_user_archived
  on agent_core.memory_topic_index(user_id, is_archived, updated_at desc);
create index if not exists idx_memory_topic_chat
  on agent_core.memory_topic_index(chat_id)
  where chat_id is not null;

-- 3.2 温记忆正文表
create table if not exists agent_core.memory_topic_content (
  id bigserial primary key,
  topic_id varchar(64) not null,
  version integer not null default 1,
  content_markdown text not null,
  content_embedding vector(1536), -- 未启用 pgvector 时可先移除该字段
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint fk_memory_topic_content_topic
    foreign key (topic_id) references agent_core.memory_topic_index(topic_id)
    on delete cascade
);

create unique index if not exists uk_memory_topic_content_topic_version
  on agent_core.memory_topic_content(topic_id, version);

-- 若已安装 pgvector，可启用向量索引（否则忽略）
-- create extension if not exists vector;
-- create index if not exists idx_memory_topic_content_vector
--   on agent_core.memory_topic_content using hnsw (content_embedding vector_cosine_ops);
```

---

## 4. MySQL DDL（用户、会话、消息、工具审计、冷记忆）

```sql
-- 建议数据库：agent_cold_memory

-- 4.1 用户主数据表
create table if not exists users (
  user_id varchar(64) primary key,
  original_user_id varchar(64) unique,
  status varchar(20) not null default 'ACTIVE', -- ACTIVE/LOCKED/ARCHIVED
  is_private boolean not null default false, -- 无痕模式
  storage_quota bigint not null default 104857600, -- 默认100MB
  created_at datetime not null default current_timestamp,
  last_active_at datetime not null default current_timestamp,
  metadata json not null,
  index idx_users_status(status),
  index idx_users_last_active(last_active_at)
) engine=InnoDB default charset=utf8mb4;

-- 4.2 会话表
create table if not exists chat_session (
  id bigint auto_increment primary key,
  chat_id varchar(64) not null unique,
  user_id varchar(64) not null,
  tenant_id varchar(64) not null default 'default',
  session_name varchar(128),
  status varchar(20) not null default 'ACTIVE',
  message_count int not null default 0,
  last_message_at datetime,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  deleted_at datetime,
  index idx_chat_session_user_created(user_id, created_at),
  index idx_chat_session_status_updated(status, updated_at),
  constraint fk_session_user_id foreign key (user_id) references users(user_id)
) engine=InnoDB default charset=utf8mb4;

-- 4.3 消息表
create table if not exists chat_message (
  id bigint auto_increment primary key,
  message_id varchar(64) not null unique,
  chat_id varchar(64) not null,
  user_id varchar(64) not null,
  tenant_id varchar(64) not null default 'default',
  role varchar(20) not null, -- SYSTEM/USER/ASSISTANT/TOOL
  message_type varchar(20) not null default 'TEXT', -- TEXT/TOOL_CALL/TOOL_RESULT/SUMMARY/EVENT
  content mediumtext not null,
  content_tokens int,
  seq_no int not null,
  metadata_json json,
  created_at datetime not null default current_timestamp,
  deleted_at datetime,
  unique key uk_chat_message_chat_seq(chat_id, seq_no),
  index idx_chat_message_user_chat_created(user_id, chat_id, created_at),
  index idx_chat_message_tenant_created(tenant_id, created_at),
  constraint fk_message_chat_id foreign key (chat_id) references chat_session(chat_id),
  constraint fk_message_user_id foreign key (user_id) references users(user_id)
) engine=InnoDB default charset=utf8mb4;

-- 4.4 工具调用审计表
create table if not exists tool_call_audit (
  id bigint auto_increment primary key,
  trace_id varchar(64),
  chat_id varchar(64),
  user_id varchar(64) not null,
  message_id varchar(64),
  tool_name varchar(64) not null,
  request_summary text,
  result_summary text,
  status varchar(20) not null, -- SUCCESS/FAILED/BLOCKED
  error_code varchar(32),
  error_message text,
  duration_ms int,
  created_at datetime not null default current_timestamp,
  index idx_tool_call_audit_user_created(user_id, created_at),
  index idx_tool_call_audit_tool_status_created(tool_name, status, created_at),
  constraint fk_audit_user_id foreign key (user_id) references users(user_id)
) engine=InnoDB default charset=utf8mb4;

-- 4.5 冷记忆原始对话表
create table if not exists cold_memory_transcripts (
  transcript_id bigint auto_increment primary key,
  user_id varchar(64) not null,
  chat_id varchar(64) not null,
  message_id varchar(64),
  query_text text not null,
  answer_text text not null,
  tool_calls json,
  rag_calls json,
  is_private boolean not null default false,
  created_at datetime not null default current_timestamp,
  is_archived boolean not null default false,
  index idx_cold_user_time(user_id, created_at desc),
  index idx_cold_chat(chat_id),
  index idx_cold_archived(is_archived),
  constraint fk_cold_user_id foreign key (user_id) references users(user_id)
) engine=InnoDB default charset=utf8mb4;

-- 4.6 可选：按月分区（数据量上来后启用）
-- alter table cold_memory_transcripts
-- partition by range (to_days(created_at)) (
--   partition p202604 values less than (to_days('2026-05-01')),
--   partition p202605 values less than (to_days('2026-06-01')),
--   partition pmax values less than maxvalue
-- );
```

---

## 5. Redis Key 规范（Hot Memory）

## 5.1 Key 设计
1. 用户全局热锚点：`agent:hot:global:{userId}`（String）
2. 会话热摘要：`agent:hot:session:{chatId}`（List）
3. 限流计数（可选）：`agent:rate:{userId}:{yyyyMMddHHmm}`

## 5.2 TTL 与容量
1. `agent:hot:global:{userId}`：TTL 30 天（每次会话续期）。
2. `agent:hot:session:{chatId}`：TTL 7~30 天（建议按业务选 14 天）。
3. 会话热摘要最多 200 条（`LPUSH + LTRIM`）。
4. 字节兜底：超过 25KB 执行尾部淘汰并追加 warning。

## 5.3 写入原则
1. 先主链路落库（PG/MySQL），再异步更新 Redis。
2. Redis 写失败不影响主流程，记录告警并重试。
3. 会话关闭后可延长保留期或立即归档（按业务策略）。

---

## 6. 写入顺序建议（避免分布式事务复杂度）
1. MySQL 事务内写：
   - `chat_message`
   - `chat_session` 计数与时间
   - `tool_call_audit`（若有）
2. MySQL 写 `cold_memory_transcripts`（失败重试队列）。
3. Redis 更新 Hot（失败重试，不阻断响应）。
4. 异步触发温记忆提炼（写 PG topic 表）。

说明：
- 不建议首期使用 2PC。
- 推荐“本地事务 + 可靠事件/重试补偿”。

---

## 7. 迁移脚本建议（Flyway/Liquibase）
1. `V1__create_pg_warm_memory.sql`：`memory_topic_index` + `memory_topic_content`
2. `V1__create_mysql_users.sql`：`users`
3. `V2__create_mysql_chat_tables.sql`：`chat_session` + `chat_message`
4. `V3__create_mysql_tool_audit.sql`：`tool_call_audit`
5. `V4__create_mysql_cold_memory.sql`：`cold_memory_transcripts`

---

## 8. 验收清单
1. 可创建用户并创建绑定 `user_id` 的会话。
2. 可写入消息并按 `chat_id` 恢复最近 N 条。
3. 可写入冷记忆并按 `user_id + chat_id` 检索。
4. Redis 热记忆可写、可截断、可过期。
5. 任一存储异常时主链路不崩溃（有降级与告警）。

