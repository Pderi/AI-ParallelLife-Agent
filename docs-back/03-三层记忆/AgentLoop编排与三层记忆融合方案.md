# AgentLoop 编排与三层记忆融合方案

> **文档定位**：在「自研 AgentLoop（多步 ReAct + 可选规划）」与「三层记忆 + RAG 单向闭环」之间建立**统一编排契约**，避免 `ParallelLifeApp` 出现两套互不相干的上下文逻辑。  
> **前提**：与 `MEMORY.md` 一致——首期 **PostgreSQL + pgvector 单库**，表与字段语义对齐 `docs-back/简化实现方案.md` **第四节**；`FileBasedChatMemory(kryo)` 为 **legacy** 路径。  
> **关联文档**：`三层记忆单库PG-本仓库代码落地方案.md`、`上下文管理改造代码落地方案.md`、`简化实现方案.md`。

---

## 一、融合原则（必须同时满足）

| 原则 | 说明 |
| --- | --- |
| **单一拼装入口** | 无论走 `ChatClient` 单次调用还是 `AgentLoop` 多步循环，**热 / 温 / 冷摘要 / RAG** 的加载顺序与优先级均由 **`ContextManager.buildContext`**（或等价方法）产出，不在 AgentLoop 内重复写 SQL 或向量检索。 |
| **单向数据流** | 与三层记忆铁则一致：**冷 → 温 → 热** 提炼方向；对话主链路只负责 **写冷**（原始 query/answer）与触发异步提炼；禁止业务代码从热层「回写」冷层伪造历史。 |
| **优先级** | 注入 LLM 的静态块顺序建议：**系统人设** → **热（global_rules + session_context + global_index 摘要）** → **温（TopK）** → **RAG（通用知识 TopK）** → **本轮用户问题**；AgentLoop 仅在首轮注入完整包，中间步不破坏该优先级（见第三节）。 |
| **会话标识** | `chatId` / `session_id` 与 `user_id` 贯穿：`ContextBuildRequest` 同时携带，保证冷记忆写入与温召回均在同一租户边界内。 |
| **provider 开关** | `memory.provider=pg|legacy`：`pg` 模式下 AgentLoop **不依赖** `MessageChatMemoryAdvisor` 维护多步列表；由 `ContextManager` + 显式 `ChatMemory`/`ColdMemoryService` 读写对齐；`legacy` 下可与现有 Advisor 并存但需约定「最终一轮写入」避免双写冲突（见第五节）。 |

---

## 二、总体架构（数据流）

```plain
                    ┌─────────────────────────────────────────┐
                    │           ParallelLifeApp               │
                    │  doChatWithAgent / doChat*（统一可选）   │
                    └────────────────────┬────────────────────┘
                                         │
                    ┌────────────────────▼────────────────────┐
                    │         ContextManager                 │
                    │  buildContext(userId, sessionId, msg)  │
                    │  → ContextPackage（热/温/RAG/冷摘要）   │
                    └────────────────────┬────────────────────┘
                                         │
              ┌──────────────────────────┼──────────────────────────┐
              │                          │                          │
              ▼                          ▼                          ▼
        hot_memory              warm_memory +              rag_knowledge /
     + cold 最近 N 轮            向量召回                    VectorStore 统一
              │                          │                          │
              └──────────────────────────┴──────────────────────────┘
                                         │
                    ┌────────────────────▼────────────────────┐
                    │      ParallelLifeAgentLoop              │
                    │  （仅消费 ContextPackage + 用户句）       │
                    │  多步：think → tool → … → 最终 assistant │
                    └────────────────────┬────────────────────┘
                                         │
                    ┌────────────────────▼────────────────────┐
                    │  afterRun：写 cold_memory（query+answer） │
                    │  + 异步 MemoryExtractService（冷→温→热）   │
                    └─────────────────────────────────────────┘
```

**要点**：AgentLoop **不直接**访问 `HotMemoryRepository` / `WarmMemoryRecallService`；只接收 **`ContextPackage`** 与 **`ToolCallback[]`**，保证三层记忆演进（加 Redis、加 MySQL）时 **只改 ContextManager 与 repository**，编排层稳定。

---

## 三、AgentLoop 与 ContextPackage 的契约

### 3.1 输入（建议类型）

| 字段 | 说明 |
| --- | --- |
| `ContextPackage` | 由 `ContextManager.buildContext` 生成：含 `systemAugmentation`（或分块字段：`hotBlock`、`warmBlock`、`ragBlock`）、`metadata`（如 `warmHitIds`、`ragHitIds` 便于观测）。 |
| `userMessage` | 本轮用户自然语言输入（与写入 `cold_memory.query` 一致）。 |
| `sessionId` / `userId` | 透传至 `afterRun`，**不**在 Loop 内持久化。 |
| `maxSteps` / `planningEnabled` | Agent 行为配置，与记忆无关。 |
| `ToolCallback[]` | 与现有 `ToolRegistration` 一致。 |

### 3.2 首轮注入策略（推荐）

1. **System**：原 `ParallelLifeApp` 人设 **拼接** `ContextPackage` 中的热/温/RAG 文本（顺序见第一节）。  
2. **User**：仅放**本轮** `userMessage`，不把整段热温 RAG 再重复塞入 User（避免 token 浪费与顺序混乱）。  
3. **多步循环内**：`messageList` 仅累积 **模型 assistant / tool** 消息；**不再**每步全量重查温/RAG（首期简化）。若后续需「每步动态召回」，应 **扩展 ContextManager** 提供 `refreshWarmRagForStep(stepContext)`，仍不分散在 AgentLoop 内写 SQL。

### 3.3 可选规划（Plan）与记忆的关系

- **规划阶段**若单独调用 LLM：输入仍应带 **同一份** `ContextPackage` 摘要，否则会出现「计划与长期记忆脱节」。  
- 规划输出可 **不入库**；最终以 **整轮最终 assistant 回答** 作为 `cold_memory.answer`（与简化方案一致）。若需审计，可扩展 `tool_call_audit` 或日志表（非首期必做）。

---

## 四、与简化方案第四节表结构的对应关系

| 表层 | AgentLoop 相关读写 |
| --- | --- |
| **hot_memory** | **读**：`ContextManager` 组装 `global_rules`、`session_context`（可由最近冷记录摘要）、`global_index`。**写**：仅异步提炼链路更新；AgentLoop **不直接写**。 |
| **warm_memory** | **读**：向量召回 TopK，进入 `ContextPackage`。**写**：仅 `MemoryExtractService`。**AgentLoop 不直接写**。 |
| **cold_memory** | **写**：整轮结束后 `afterRun` 写一条 `query` + `answer`（answer = AgentLoop 最终面向用户的文本）。**多步工具中间结果** 默认不逐条写入冷层，避免冷层爆炸；若产品需要，可配置「仅最终」或「最终 + 工具摘要」策略。 |
| **rag_knowledge** | **读**：与 RAG 统一策略一致（`RagRecallService` 或 `VectorStore` 二选一，由 `ContextManager` 统一）。**写**：与 Agent 无关，走文档入库任务。 |
| **users** | 会话创建与 `user_id` 绑定，与 `ParallelLifeController` / `ChatRequest` 扩展一致。 |

---

## 五、legacy（kryo）与 pg 双模式下的行为

| 模式 | AgentLoop 建议 |
| --- | --- |
| **pg** | 去掉对 `MessageChatMemoryAdvisor` 的依赖；多步上下文仅靠 **AgentLoop 内部 `messageList`**；**轮次结束** `ColdMemoryService.append` + 可选同步更新 `hot_memory.session_context`（若实现会话级热更新）。 |
| **legacy** | 若仍启用 Advisor：**避免** Advisor 与 AgentLoop 同时维护两套历史。推荐：**AgentLoop 路径关闭该 Advisor**，仅在本轮结束时手动 `FileBasedChatMemory.add(conversationId, ...)` 写入 **一条 user + 一条 assistant**（与冷层逻辑对齐时可二选一主存）。文档层面约定：**以未来 pg 单路径为准**，legacy 为过渡。 |

---

## 六、建议包结构与类职责（在现有落地方案上增量）

在 `三层记忆单库PG-本仓库代码落地方案.md` 第三节基础上 **新增**：

| 路径 | 职责 |
| --- | --- |
| `orchestration/AgentLoopRequest.java` | 承载 `ContextPackage`、`userMessage`、`userId`、`sessionId`、工具与 `maxSteps` 等。 |
| `orchestration/AgentLoopResult.java` | `finalAnswer`、`traces`、`FinishReason`。 |
| `orchestration/ParallelLifeAgentLoop.java` | `@Component`，无请求级共享可变状态；`run(AgentLoopRequest)` 内新建 `messageList`，使用 `ToolCallingManager` + `DashScopeChatOptions.withProxyToolCalls(true)`（与现有 `ToolCallAgent` 技术栈对齐）。 |
| `context/ContextManager.java`（已有规划） | **增加** `buildContextForAgent(...)` 若与普通 chat 参数一致可复用 `buildContext`；保证 RAG 只灌一次。 |
| `app/ParallelLifeApp.java` | `doChatWithAgent`：`ContextManager.buildContext` → `ParallelLifeAgentLoop.run` → `ContextManager.afterRun`（写冷 + 异步提炼）。 |

**不继承** `BaseAgent`/`ToolCallAgent`，避免单例与状态污染；**叙事上**仍属同一项目的 ReAct 范式。

---

## 七、与 RAG、工具的交叉

- **RAG**：必须在 **ContextManager** 侧完成召回并进入 `ContextPackage`；AgentLoop 内 **不** 直接挂 `QuestionAnswerAdvisor`（Advisor 绑定 `ChatClient.prompt` 与自管 `Prompt(messageList)` 模型不一致）。  
- **工具**：工具调用事实若需审计，可在 `afterRun` 或 AOP 写入 `tool_call_audit`（表结构见持久化文档）；与 AgentLoop 的 `StepTrace` 可互相对照。  
- **ForbiddenWordAdvisor / MyLoggerAdvisor**：单次 `ChatClient` 路径继续可用；AgentLoop 若用底层 `ChatModel`/`ChatClient` 调用，违禁词可在 **出参** 侧复用同一过滤器或在 `ContextManager` 后处理（择一统一）。

---

## 八、异步闭环（冷 → 温 → 热）

AgentLoop 整轮结束后，`ContextManager.afterRun` 建议行为与纯 Chat 路径 **完全一致**：

1. 写入 `cold_memory`（`query` / `answer` / `user_id` / `session_id`）。  
2. 满足阈值时触发 `MemoryExtractService.extractColdToWarm`（异步）：提炼温记忆、更新 `hot_memory.global_index`。  
3. 失败策略：记录日志 + `ERROR.md` 规范；对用户仍返回本轮 `AgentLoopResult`（可降级不打断主链路）。

这样 **无论单步 Chat 还是多步 Agent**，三层记忆的「越用越懂你」叙事 **统一**。

---

## 九、验收清单（融合向）

- [ ] `memory.provider=pg` 时，`doChatWithAgent` 后 `cold_memory` 有对应 `query/answer`，且 `answer` 为 Agent 最终回复。  
- [ ] 同一会话第二次请求，`ContextManager` 召回的温/热内容能体现上一轮主题（依赖提炼异步完成时可测延迟场景）。  
- [ ] `ContextPackage` 的拼装顺序符合 **热 > 温 > RAG** 优先级（可通过单测快照或日志断言）。  
- [ ] AgentLoop 多步工具调用不产生 **重复** 冷记录（除非明确开启「每步落冷」配置）。  
- [ ] 并发两 `sessionId` 无 `messageList` 串话（AgentLoop 每请求新建列表）。

---

## 十、扩展期对齐（非首期）

- **Redis 热层**：仅替换 `ContextManager` 读热路径；AgentLoop 契约不变。  
- **MySQL 会话主链**：`session` / `message` 若迁移，`afterRun` 双写或切换数据源，AgentLoop 仍只认 `ContextManager.afterRun`。  
- **MQ + Outbox**：提炼与索引更新走消息队列时，触发点仍在 `afterRun` 之后，与是否 AgentLoop 无关。

---

## 十一、文档索引

| 文档 | 用途 |
| --- | --- |
| `简化实现方案.md` 第四节 | 单库 PG 表结构 |
| `三层记忆单库PG-本仓库代码落地方案.md` | ContextManager、Repository、ParallelLifeApp 改造清单 |
| `上下文管理改造代码落地方案.md` | 分阶段与模块边界 |
| `简化实现方案-开发任务清单与进度追踪.md` | P1–P5 与里程碑 |

---

**版本说明**：本文档描述「编排层 + 三层记忆」的融合设计与验收口径；具体 Java 实现以仓库落地代码为准，实施后请在 `MEMORY.md`「最近已完成改造」中追加可验证条目。
