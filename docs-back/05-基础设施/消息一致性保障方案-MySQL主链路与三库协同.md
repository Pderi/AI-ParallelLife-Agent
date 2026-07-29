# 消息一致性保障方案（MySQL主链路与三库协同）

## 1. 文档目的
本方案用于说明在当前三库架构下如何保障消息一致性：
1. MySQL：会话、消息、工具审计、冷记忆主链路
2. PostgreSQL：Warm 记忆与 RAG 结构化数据
3. Redis：Hot 记忆缓存与短期状态

目标：
- 保证“消息不丢、不重、可追踪、可恢复”
- 发生单点故障时主链路可降级、最终可一致

---

## 2. 一致性分层目标

## 2.1 强一致（事务内）
范围：MySQL 主链路内的同一次请求写入。

必须保证：
1. `chat_message` 与 `chat_session` 计数/时间一致。
2. `tool_call_audit` 与对应 `message_id` 可关联。

实现方式：
- 单库本地事务（MySQL InnoDB 事务）一次提交。

## 2.2 最终一致（跨库）
范围：MySQL -> Redis / PostgreSQL 的异步更新。

允许短暂不一致，但必须最终修复：
1. Redis 热记忆暂时落后于 MySQL 消息。
2. Warm 记忆提炼延迟写入 PostgreSQL。

实现方式：
- Outbox + 重试 + 幂等 + 对账修复。

---

## 3. 核心写入时序（标准）

```plain
请求进入
  -> 生成 request_id / message_id / trace_id
  -> MySQL事务开始
      1) 写 chat_message (USER/ASSISTANT/TOOL)
      2) 更新 chat_session(message_count,last_message_at)
      3) 写 tool_call_audit(若有)
      4) 写 outbox_event (HOT_UPDATE / WARM_EXTRACT / COLD_APPEND)
     MySQL事务提交
  -> 返回响应（主链路完成）
  -> 异步消费者处理 outbox_event
      A) 更新 Redis Hot
      B) 写/更 Warm(PostgreSQL)
      C) 写 cold_memory_transcripts(MySQL扩展)
  -> 失败重试 + 死信 + 对账任务修复
```

关键点：
1. 主响应依赖 MySQL 事务成功，不依赖跨库写入成功。
2. 跨库动作全部走事件化，避免分布式事务。

---

## 4. 关键保障机制

## 4.1 幂等设计（防重）
1. `message_id` 全局唯一，重复请求直接幂等返回。
2. `tool_call_audit` 通过 `(trace_id, tool_name, message_id)` 去重。
3. Outbox 事件有 `event_id`，消费者记录 `processed_event_id` 防重放。

建议唯一约束：
1. `chat_message.message_id unique`
2. `chat_message(chat_id, seq_no) unique`
3. `outbox_event.event_id unique`
4. `processed_event.consumer_name + event_id unique`

## 4.2 顺序保障（防乱序）
1. 会话内 `seq_no` 单调递增（数据库分配或业务加锁）。
2. 同一 `chat_id` 事件按分区键投递，保证同会话串行消费。
3. Redis 会话热摘要更新按 `seq_no` 防回退。

## 4.3 重试与死信（防丢）
1. 指数退避重试：1s/5s/30s/2m/10m。
2. 超过最大重试进入死信队列（DLQ）。
3. 死信人工/自动补偿后可回放。

## 4.4 对账修复（兜底）
定时任务每 5~15 分钟执行：
1. 扫描 MySQL 中“已提交但未处理完”的 outbox 事件。
2. 校验 Redis 热记忆版本是否落后。
3. 校验 Warm topic 是否缺失关键提炼结果。
4. 自动补发事件，必要时告警。

---

## 5. 数据模型补充（建议）

## 5.1 Outbox 事件表（MySQL）
```sql
create table if not exists outbox_event (
  id bigint auto_increment primary key,
  event_id varchar(64) not null unique,
  event_type varchar(32) not null, -- HOT_UPDATE/WARM_EXTRACT/COLD_APPEND
  aggregate_type varchar(32) not null, -- CHAT_MESSAGE/SESSION
  aggregate_id varchar(64) not null,   -- message_id/chat_id
  user_id varchar(64) not null,
  chat_id varchar(64) not null,
  payload json not null,
  status varchar(16) not null default 'NEW', -- NEW/PROCESSING/SUCCESS/FAILED/DLQ
  retry_count int not null default 0,
  next_retry_at datetime,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  index idx_outbox_status_next(status, next_retry_at),
  index idx_outbox_user_chat(user_id, chat_id, created_at)
) engine=InnoDB default charset=utf8mb4;
```

## 5.2 消费幂等表（MySQL）
```sql
create table if not exists event_consume_log (
  id bigint auto_increment primary key,
  consumer_name varchar(64) not null,
  event_id varchar(64) not null,
  consumed_at datetime not null default current_timestamp,
  unique key uk_consumer_event(consumer_name, event_id)
) engine=InnoDB default charset=utf8mb4;
```

---

## 6. 失败场景与处理策略

## 场景 A：MySQL 事务失败
现象：消息未写入，接口报错。
处理：直接失败返回，不触发异步；由调用方重试（幂等 message_id）。

## 场景 B：MySQL 成功，Redis 失败
现象：主响应成功，热记忆暂时缺失。
处理：Outbox 重试补写 Redis；若超时，降级走 MySQL 最近消息构建 Hot 最小模板。

## 场景 C：MySQL 成功，PostgreSQL 失败
现象：主响应成功，Warm 更新延迟。
处理：Outbox 重试 Warm 提炼；召回时先关键词 + 旧 topic；对账任务补修。

## 场景 D：消费者重复消费
现象：可能重复写。
处理：幂等日志表拦截，重复事件直接 ack。

## 场景 E：并发写同一会话
现象：`seq_no` 冲突。
处理：唯一约束 + 短重试；必要时按 `chat_id` 分布式锁（仅会话维度）。

---

## 7. 一致性校验指标（必须监控）
1. `message_write_success_rate`（主链路写成功率）
2. `outbox_lag_seconds_p95`（事件延迟）
3. `outbox_retry_rate`（重试率）
4. `dlq_count`（死信数量）
5. `hot_stale_session_count`（Hot 落后会话数）
6. `warm_extract_backlog`（Warm 提炼积压）
7. `session_message_count_diff`（会话计数与消息实际数量差值）

告警阈值建议：
1. `dlq_count > 0` 立即告警
2. `outbox_lag_seconds_p95 > 60` 告警
3. `session_message_count_diff != 0` 持续 5 分钟告警

---

## 8. 代码落地建议（本项目）
1. 在 `ChatMessageService` 内实现主事务写（消息+会话+审计+outbox）。
2. 新增 `OutboxDispatcher`（轮询或消息队列消费）。
3. 新增 `HotMemoryUpdater`（Redis 写入）与 `WarmMemoryUpdater`（PG 写入）。
4. 新增 `ConsistencyReconcileJob`（定时对账补偿）。
5. 所有异常通过 `BusinessException + ErrorCode` 统一语义输出。

---

## 9. 验收标准
1. 压测 10w 条消息无丢失、无重复（按 `message_id` 验证）。
2. 故障注入（断 Redis/断 PG）下主链路可用率 >= 99.9%。
3. 恢复后 15 分钟内通过重试+对账恢复最终一致。
4. `chat_session.message_count` 与 `chat_message` 计数一致率 100%。
5. 所有跨库失败都可追踪到 `outbox_event` 与 `event_consume_log`。

---

## 10. 结论
在“消息主链路落 MySQL、Warm 在 PostgreSQL、Hot 在 Redis”的架构下，
最可靠的一致性策略是：
1. MySQL 内强一致事务；
2. 跨库事件化最终一致（Outbox）；
3. 幂等 + 重试 + 死信 + 对账四件套闭环。

该方案可在不引入分布式事务的前提下，达到工程上可落地、可维护、可恢复的一致性保障水平。

