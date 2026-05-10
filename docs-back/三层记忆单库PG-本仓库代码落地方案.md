# 三层记忆（单库 PostgreSQL）— 本仓库代码落地方案

> **前提**：与 `MEMORY.md` 一致——首期 **仅 PostgreSQL + pgvector**，表结构对齐 `docs-back/简化实现方案.md` **第四节**；`FileBasedChatMemory(kryo)` 保留为 **legacy** 开关路径。  
> **任务追踪**：`docs-back/简化实现方案-开发任务清单与进度追踪.md`  
> **包根**：`src/main/java/com/qin/qaiagentproject`

---

## 一、当前代码与改造关系（一张图）

```plain
ParallelLifeController  ──►  ParallelLifeApp.doChat* / doChatWithRag* / doChatWithTools*
                                    │
                    ┌───────────────┴───────────────┐
                    │ 首期：ContextManager 拼装    │
                    │ + PG 读写冷/温/热            │
                    │ + ChatClient 调用            │
                    └───────────────┬───────────────┘
                                    │
              legacy：MessageChatMemoryAdvisor + FileBasedChatMemory
```

**Agent 子包**（`BaseAgent` / `ToolCallAgent` / `ReActAgent`）：与主 API 链路**相对独立**；首期可**不动**或仅后续把「工具对话」也接入同一 `ContextManager`。

---

## 二、依赖与工程脚手架（要改的文件）

| 动作 | 路径 | 说明 |
| --- | --- | --- |
| **修改** | `pom.xml` | 启用/新增：`spring-boot-starter-jdbc`、`postgresql`（runtime）、`flyway-core`（或 Flyway Spring Boot starter）；与 Spring AI 版本对齐的 **pgvector** 依赖（`pom.xml` 内已有注释块，按需取消注释并统一版本）。 |
| **新增** | `src/main/resources/db/migration/V1__init_three_layer_memory.sql`（示例名） | Flyway 脚本：对齐简化方案第四节 DDL（`vector` 扩展 + 五张表）。 |
| **修改** | `src/main/resources/application.yml` 或 `application-local.yml` | `spring.datasource.*` 指向 PostgreSQL；`spring.flyway.enabled`、`locations`；**勿提交**真实密码。 |
| **新增（可选）** | `src/main/resources/application-memory.yml` | `memory.provider=pg|legacy` 等开关，便于回滚 kryo。 |

---

## 三、建议新增包与类（首期）

以下路径均相对于 `com.qin.qaiagentproject`。

### 3.1 持久化访问层

| 建议路径 | 职责 |
| --- | --- |
| `memory/entity/` 或 `persistence/entity/` | 与表对应的 POJO / Record（`UserEntity`、`HotMemoryEntity`、`WarmMemoryEntity`、`ColdMemoryEntity`、`RagChunkEntity`）—— 也可用 **JdbcTemplate + RowMapper** 不写实体。 |
| `memory/repository/` | `UserRepository`、`HotMemoryRepository`、`WarmMemoryRepository`、`ColdMemoryRepository`、`RagKnowledgeRepository`：CRUD + 向量检索 SQL（`<->` 或参数化向量）。 |

> 若偏好 MyBatis-Plus / JPA，可改为 `mapper/` 或 `jpa/` 包，但需与 Flyway DDL 一致。

### 3.2 领域服务

| 建议路径 | 职责 |
| --- | --- |
| `memory/service/UserMemoryBootstrapService.java` | 新用户 `INSERT hot_memory`（`ON CONFLICT DO NOTHING`），对齐简化方案 `initUserMemory`。 |
| `memory/service/ColdMemoryService.java` | `appendColdMemory(userId, sessionId, query, answer)`。 |
| `memory/service/WarmMemoryRecallService.java` | `recallWarmMemory(userId, query, topK)`，向量检索。 |
| `memory/service/RagRecallService.java` | 读 `rag_knowledge` TopK；**或与** 现有 `VectorStore` **二选一**为主数据源，避免双份知识库（见下文「与现有 RAG 关系」）。 |
| `memory/service/MemoryExtractService.java` | 异步 `extractColdToWarm`：读 `cold_memory`、调 `ChatModel` JSON 提炼、写 `warm_memory`、更新 `hot_memory.global_index`。 |

### 3.3 上下文编排（核心）

| 建议路径 | 职责 |
| --- | --- |
| `context/ContextBuildRequest.java` | `userId`、`sessionId`（UUID 字符串）、`message`、`useRag`、`useTools` 等。 |
| `context/ContextPackage.java` | 拼装后的各块文本 + 元数据。 |
| `context/ContextManager.java` | `buildContext(request)`：`getHotMemory`（`hot_memory` + 最近 N 条 `cold_memory`）+ 并行 Warm/RAG；`afterRun`：写冷、触发异步提炼。 |
| `context/PromptAssembler.java`（可选） | 固定顺序：系统设定 → 热 → 温 → RAG → 会话上下文 → 用户问题。 |

### 3.4 异步与配置

| 建议路径 | 职责 |
| --- | --- |
| `config/AsyncConfig.java` | `@EnableAsync`，提炼任务线程池，避免阻塞 Web 线程。 |
| `config/MemoryProperties.java` | `@ConfigurationProperties`：`provider`、`warmTopK`、`coldRecentRounds`、`extractMinTurns` 等。 |

---

## 四、必须修改的现有文件

| 文件 | 改造要点 |
| --- | --- |
| `app/ParallelLifeApp.java` | **核心改造点**。（1）构造或注入：双模式 `ChatClient`—— **legacy** 仍带 `MessageChatMemoryAdvisor(FileBasedChatMemory)`；**pg** 模式去掉该 Advisor 或条件注册。（2）`doChat` / `doChatStream` / `doChatWithRag` / `doChatWithTools`：在 `prompt` 前调用 `ContextManager.buildContext`，将结果注入 `system`/`user` 或 `advisors`；调用后 `afterRun`。（3）RAG 路径：避免与自研 `RagRecallService` 重复灌入——统一走 `ContextManager` 或仅保留 `QuestionAnswerAdvisor` 之一。 |
| `controller/ParallelLifeController.java` | （1）`createSession`：若需绑定 `userId`，从 `CreateSessionRequest` 读或生成匿名 ID，并写入 PG `users` + `hot_memory`。（2）对话接口：将 `userId` 传入 `ParallelLifeApp`（若 DTO 暂无则扩展，见下）。 |
| `dto/ChatRequest.java` | **建议新增** `userId`（可先可选；缺省时后端生成匿名 UUID 并返回/落 cookie 由你产品决定）。`chatId` 与简化方案 `session_id` 语义对齐（均为字符串 UUID）。 |
| `dto/CreateSessionRequest.java` | 确认是否含 `userId`；与会话创建落库一致。 |
| `dto/SessionResponse.java` | 若增加 `userId` 返回，便于前端持久化。 |

---

## 五、按需修改 / 谨慎触碰

| 文件 | 说明 |
| --- | --- |
| `chatmeomery/FileBasedChatMemory.java` | **保留**；仅 legacy 路径使用；不要在首期删除。 |
| `rag/ParallelLifeVectorStoreConfig.java`、`rag/ParallelLifeDocumentLoader.java` | 若 RAG 改为 **`rag_knowledge` 表 + 自研检索**，需决定：继续用 Spring AI `VectorStore` 同步写入 PG，还是只用 Jdbc 读 `rag_knowledge`。**二选一为主**，并在 `ContextManager` 统一。 |
| `advisor/MyLoggerAdvisor.java`、`advisor/ForbiddenWordAdvisor.java` | 保留；`ParallelLifeApp` 构建 `ChatClient` 时继续挂上。 |
| `tools/*`、`tools/ToolRegistration.java` | 首期可不改；工具对话记忆若要与 PG 冷层一致，后续在 `doChatWithTools` 的 `afterRun` 里同样写 `cold_memory`。 |
| `agent/BaseAgent.java`、`agent/ToolCallAgent.java`、`agent/ReActAgent.java` | **首期可不动**；与 `ParallelLifeController` 主链路并行存在。若统一体验，可后续增加「从 `ContextManager` 取 system 前缀注入 `messageList`」。 |
| `exception/ErrorCode.java`、`exception/BusinessException.java` | 按需新增：如 `USER_NOT_FOUND`、`MEMORY_EXTRACT_FAILED`（业务上可对用户降级为成功，仅日志）。 |
| `QAiAgentProjectApplication.java` | 若使用 `@EnableAsync`、`@EnableConfigurationProperties`，在此或 `config` 包启用。 |

---

## 六、测试与本地脚本

| 路径 | 说明 |
| --- | --- |
| `src/test/java/.../context/ContextManagerTest.java` | Mock Repository，测 Prompt 顺序与空数据。 |
| `src/test/java/.../memory/...RepositoryTest.java` | Testcontainers PostgreSQL + pgvector（推荐）或手工集成环境。 |

---

## 七、实施顺序（与任务清单 P1–P5 对应）

1. `pom.xml` + `application-local.yml` + Flyway `V1__*.sql`  
2. `memory/repository/*` + `UserMemoryBootstrapService` + `ColdMemoryService`  
3. `ContextManager` + 改造 `ParallelLifeApp`（开关 legacy）  
4. `MemoryExtractService` + `AsyncConfig`  
5. `ParallelLifeController` / `ChatRequest` 补齐 `userId` 流  
6. 与现有 `VectorStore` 做 RAG 统一策略  

---

## 八、扩展期（非首期）可再动的代码

- 引入 **MySQL** 第二数据源：新增 `config/DataSourceConfig`、迁移会话表；`ParallelLifeApp` 写库切换。  
- **Redis** 热层：新增 `memory/redis/HotMemoryRedisRepository`，`ContextManager` 读路径优先 Redis。  
- **Outbox + MQ**：见 `消息一致性保障方案`，新增 `outbox` 包与消费者。

---

## 九、验收（与本仓库相关）

- [ ] `memory.provider=pg` 时，对话后 `cold_memory` 有记录。  
- [ ] 多轮后 `warm_memory` 有更新，二次提问召回生效。  
- [ ] `memory.provider=legacy` 时行为与当前 kryo 一致。  
- [ ] 无数据库时启动行为明确（快速失败或自动 fallback，需在配置中约定）。

---

## 十、文档索引

- 表结构：`docs-back/简化实现方案.md` 第四节  
- 进度：`docs-back/简化实现方案-开发任务清单与进度追踪.md`  
- 环境：`docs-back/基础设施与数据库环境准备-任务清单.md`
