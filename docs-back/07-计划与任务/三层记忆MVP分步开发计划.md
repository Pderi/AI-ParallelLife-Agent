# 三层记忆 MVP 分步开发计划

> **依据文档**：`docs-back/03-三层记忆/封装三层记忆模型落地方案(最终标准) .md`（生产级蓝图）  
> **落地仓库**：`q-Ai-agent-project`（Spring Boot 3 + Spring AI Alibaba + Java 21）  
> **首期策略**：与 `MEMORY.md` 一致 — **仅 PostgreSQL + pgvector 单库**，能用一个就不拆多个  
> **MVP 目标**：跑通「提问 → 热/温/RAG/冷上下文拼装 → LLM 回答 → 写冷 → 异步提炼温 → 更新热索引」全链路，可演示、可验收  
> **非目标（MVP 不做）**：Redis / MySQL / Qdrant / MinIO / MQ / Outbox / 多租户 RLS / 跨会话冷检索 / AgentLoop 多步编排

---

## 一、MVP 与生产级方案的映射

参考文档中的「黄金组合」存储（Redis + PG + Qdrant + MySQL + MinIO）在 MVP 阶段**全部收敛到一张 PostgreSQL**，用表分工代替多组件：

| 生产级概念 | MVP 单库落地 | MVP 简化说明 |
| --- | --- | --- |
| Redis 热记忆 | `hot_memory` 表 | 每次对话 `SELECT` 读，不做微秒级缓存 |
| PG + Qdrant 温记忆 | `warm_memory` 表 + `content_vector` + pgvector | 不用独立向量库 |
| PG RAG 知识库 | `rag_knowledge` 表 + `doc_vector` | 替代现有 `SimpleVectorStore` 内存库 |
| MySQL + MinIO 冷记忆 | `cold_memory` 表 | 仅存文本，不做对象存储与分区 |
| Memory Manager 中枢 | `ContextManager` + 若干 Service | 不做独立微服务 |
| 异步提炼队列 | `@Async` + 线程池 | 不用 MQ |
| 当前会话滑动窗口 | 最近 3 轮 `cold_memory` 全文 | 不做「>3 轮压缩摘要」 |
| 跨会话冷检索 | **不做** | 二期按需加 |
| 温记忆引导 RAG（模式 2/3/4） | **不做** | MVP 仅「并行召回 + 融合」（模式 1） |

**MVP 仍必须遵守的铁则**（来自参考文档）：

1. **先写原则**：每轮对话先写 `cold_memory`，再触发上层更新  
2. **单向流动**：冷 → 温 → 热，禁止反向  
3. **召回优先级**：热 > 温 > RAG > 冷·当前会话  
4. **异步解耦**：提炼不阻塞主链路  
5. **边界清晰**：RAG 存通用知识，温记忆存用户个性化内容  

---

## 二、MVP 全链路时序（验收对照）

```plain
用户 POST /parallel-life/chat
    │
    ▼
① 确保 userId 存在 → users + hot_memory 初始化
    │
    ▼
② ContextManager.buildContext（并行）
    ├─ Hot：hot_memory.global_rules + global_index + 最近3轮 cold_memory
    ├─ Warm：warm_memory 向量 Top3
    └─ RAG：rag_knowledge 向量 Top3
    │
    ▼
③ PromptAssembler 固定顺序拼装 → ChatClient 调用 LLM
    │
    ▼
④ afterRun：INSERT cold_memory（query + answer）
    │
    ▼
⑤ @Async extractColdToWarm（会话 ≥2 轮时）
    ├─ LLM JSON 提炼 → UPSERT warm_memory + 向量
    └─ UPDATE hot_memory.global_index
```

**MVP 跑通标准（手工验收）**：

- [ ] 第 1 轮对话后 `cold_memory` 有 1 条记录  
- [ ] 第 2～3 轮后 `warm_memory` 出现提炼条目（或日志可见异步任务执行）  
- [ ] 第 3 轮同类问题回答能引用温记忆内容  
- [ ] `useRag=true` 时能从 `rag_knowledge` 召回（需预置几条向量数据）  
- [ ] `memory.provider=legacy` 时仍走 kryo，行为与现网一致  

---

## 三、分步开发计划（按执行顺序）

> **原则**：一步一验收，通过后再进入下一步；每步改动面可控、可回滚。

---

### 第一步：基础设施与数据库脚手架（P0 + P1）

**目标**：本地 PostgreSQL + pgvector 可用，Flyway 迁移成功，应用能连库启动。

**做什么**：

| 序号 | 任务 | 涉及文件/路径 |
| --- | --- | --- |
| 1.1 | Docker 或本机拉起 PG 15+，`CREATE EXTENSION vector` | 见 `基础设施与数据库环境准备-任务清单.md` Phase C |
| 1.2 | `pom.xml` 取消注释：`spring-boot-starter-jdbc`、`postgresql`、`flyway-core` | `pom.xml` |
| 1.3 | 新增 Flyway 脚本：五张表 + 向量索引 | `src/main/resources/db/migration/V1__init_three_layer_memory.sql` |
| 1.4 | 配置数据源（密码不入库） | `application-local.yml`（或 gitignore 的 local secrets） |
| 1.5 | 确认 embedding 向量维度（DashScope 默认，与 DDL 列维度一致） | 文档备注 + 配置 |

**DDL 来源**：`docs-back/02-架构与方案/简化实现方案.md` **第四节**（`users`、`hot_memory`、`warm_memory`、`cold_memory`、`rag_knowledge`）。

**本步不做**：业务 Java 代码、ContextManager、改造 ParallelLifeApp。

**验收**：

- `mvn spring-boot:run` 启动无 Flyway 报错  
- `\dt` 可见五张表；`\dx` 含 `vector`  
- 手工 `INSERT` 一条向量可 `ORDER BY content_vector <-> ...` 查询  

**预估**：0.5～1 人天  

---

### 第二步：持久化访问层 + 用户/冷记忆基础写读（P2 前半）

**目标**：能创建用户、初始化热记忆、写入/读取冷记忆；尚无 ContextManager。

**做什么**：

| 序号 | 任务 | 建议类 |
| --- | --- | --- |
| 2.1 | 新增 `memory/repository/*Repository`（JdbcTemplate） | `HotMemoryRepository`、`ColdMemoryRepository`、`UserRepository` |
| 2.2 | `UserMemoryBootstrapService`：`INSERT hot_memory ON CONFLICT DO NOTHING` | 对齐参考文档 initUserMemory |
| 2.3 | `ColdMemoryService.append(userId, sessionId, query, answer)` | 每轮落盘 |
| 2.4 | `HotMemoryService.getHot(userId, sessionId)` | 读 global_rules/index + 最近 N 条 cold |
| 2.5 | 新增 `MemoryProperties`（topK、recentRounds 等） | `config/MemoryProperties.java` |
| 2.6 | 单元/集成冒烟测试 | Testcontainers 或本地 PG |

**本步不做**：向量检索、LLM 提炼、ParallelLifeApp 改造。

**验收**：

- 调用 Service 后 `users` + `hot_memory` 行存在  
- `appendColdMemory` 后 `cold_memory` 可查  
- `getHot` 返回最近 3 轮会话文本  

**预估**：1～1.5 人天  

---

### 第三步：向量嵌入 + 温/RAG 检索（P3 前半）

**目标**：给定 query，能从 `warm_memory` 和 `rag_knowledge` 各 TopK 召回；仍不接对话主链路。

**做什么**：

| 序号 | 任务 | 建议类 |
| --- | --- | --- |
| 3.1 | `EmbeddingService`：封装现有 `EmbeddingModel`（DashScope） | 统一 `float[]` / PG 向量字面量转换 |
| 3.2 | `WarmMemoryRecallService.recall(userId, query, topK)` | SQL `<->` 余弦距离 |
| 3.3 | `RagRecallService.recall(query, topK)` | 读 `rag_knowledge` |
| 3.4 | RAG 种子数据：启动时或 Flyway `V2__seed_rag_sample.sql` 导入 3～5 条 | 替代 `SimpleVectorStore` 演示数据 |
| 3.5 | Repository 集成测试 | 插入向量 + 检索命中 |

**RAG 策略（MVP 定案）**：

- **主数据源**：`rag_knowledge` 表 + Jdbc 检索  
- **停用路径**：`ParallelLifeVectorStoreConfig` 的 `SimpleVectorStore` 在 `memory.provider=pg` 时不注入对话链路  
- **不双写**：避免 PG 表与 SimpleVectorStore 各存一份  

**本步不做**：Prompt 拼装、ParallelLifeApp 改造。

**验收**：

- 预置 RAG 文档后，相似问题能召回对应 chunk  
- 写入 warm_memory 后，语义相近 query 能 TopK 命中  

**预估**：1.5～2 人天  

---

### 第四步：ContextManager 上下文编排（P3 后半）

**目标**：离线可调 `buildContext`，输出固定顺序的完整 Prompt  augmentation。

**做什么**：

| 序号 | 任务 | 建议类 |
| --- | --- | --- |
| 4.1 | `ContextBuildRequest` / `ContextPackage` | userId、sessionId、message、useRag 等 |
| 4.2 | `ContextManager.buildContext(request)` | 并行 Hot + Warm + RAG（`CompletableFuture`） |
| 4.3 | `PromptAssembler`（可选独立类） | 顺序：系统人设 → 热 → 温 → RAG → 会话上下文 → 用户问题 |
| 4.4 | Golden Case 单测 | 空数据 / 仅热 / 温+RAG 齐套 |

**Prompt 模板**：对齐参考文档「模式 1：并行召回+结果融合」模板（第十章）。

**本步不做**：改 Controller、不改 ParallelLifeApp。

**验收**：

- 单测快照：各块顺序与参考文档一致  
- 空库降级：缺温/缺 RAG 时不抛异常，对应块为空字符串  

**预估**：1.5～2 人天  

---

### 第五步：对话主链路接入（P4 — MVP 核心）

**目标**：`POST /parallel-life/chat` 走三层记忆；对话后写冷记忆；保留 legacy 回滚。

**做什么**：

| 序号 | 任务 | 涉及文件 |
| --- | --- | --- |
| 5.1 | `ChatRequest` 增加 `userId`（可选；缺省后端生成匿名 UUID） | `dto/ChatRequest.java` |
| 5.2 | `createSession`：落库 user + 初始化 hot_memory | `ParallelLifeController.java` |
| 5.3 | 改造 `ParallelLifeApp.doChat`（先非流式） | buildContext → 注入 system → call → afterRun |
| 5.4 | `memory.provider=pg\|legacy` 条件装配 | `application-memory.yml` 或 local profile |
| 5.5 | pg 模式：移除/旁路 `MessageChatMemoryAdvisor` | 避免与 cold_memory 双写 |
| 5.6 | 流式 `doChatStream`（第二步接入，首 token 前完成 buildContext） | 同文件 |

**本步不做**：异步提炼（下一步）、工具路径、报告结构化输出。

**验收**：

- `memory.provider=pg`：对话成功，`cold_memory` 每轮 +1  
- `memory.provider=legacy`：kryo 行为不变  
- Swagger 可测通 `/parallel-life/chat`  

**预估**：2～3 人天  

---

### 第六步：异步冷→温→热提炼闭环（P5 — MVP 闭环）

**目标**：多轮对话后温记忆可沉淀，热索引更新；主链路不阻塞。

**做什么**：

| 序号 | 任务 | 建议类 |
| --- | --- | --- |
| 6.1 | `config/AsyncConfig.java` | `@EnableAsync` + 线程池 |
| 6.2 | `MemoryExtractService.extractColdToWarm(userId, sessionId)` | 读 cold → LLM JSON → upsert warm + 向量 |
| 6.3 | 更新 `hot_memory.global_index` | jsonb_set |
| 6.4 | `ContextManager.afterRun` 内触发异步（阈值：会话 ≥2 条 cold） | 失败仅打日志 |
| 6.5 | 提炼 Prompt | 对齐 `简化实现方案.md` 第五节 extractPrompt |

**MVP 简化**：

- 触发时机：每轮 afterRun 后**立即异步**（不做「会话结束 1 分钟」延迟）  
- 不做批量凌晨任务、不做模式 3「RAG 反哺温记忆」  

**验收**：

- 同一 session 聊 2～3 轮后 `warm_memory` 有记录  
- `global_index` JSON 含 topic → scene 映射  
- 第 3 轮问相关话题，Warm 块非空且回答更贴合  

**预估**：2～3 人天  

---

### 第七步：RAG 开关 + 端到端联调（P6 精简）

**目标**：`useRag=true` 走统一 ContextManager；导入项目领域文档向量；全链路演示通过。

**做什么**：

| 序号 | 任务 | 说明 |
| --- | --- | --- |
| 7.1 | `doChatWithRag` 合并进 ContextManager 路径 | 去掉重复 `QuestionAnswerAdvisor` 双灌 |
| 7.2 | 文档入库脚本或 Admin 接口（二选一，MVP 推荐 Flyway seed + 一次性 Java `@PostConstruct` loader） | 人生规划/行业趋势类 markdown 切片 |
| 7.3 | 手工 E2E 用例文档 | 3 个 scenario 写进 README 或本计划附录 |
| 7.4 | （可选）最小集成测试 | Mock ChatModel + 真 PG |

**本步不做**：工具调用记忆同步、AgentLoop。

**验收**：

- Scenario A：纯对话，冷记忆累积  
- Scenario B：多轮后温记忆生效  
- Scenario C：useRag=true，回答含知识库事实  

**预估**：1～2 人天  

---

## 四、MVP 代码包结构（首期新增）

```plain
com.qin.qaiagentproject
├── config/
│   ├── AsyncConfig.java
│   └── MemoryProperties.java
├── context/
│   ├── ContextBuildRequest.java
│   ├── ContextPackage.java
│   ├── ContextManager.java          ← Memory Manager MVP 等价物
│   └── PromptAssembler.java         ← 可选
├── memory/
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── HotMemoryRepository.java
│   │   ├── ColdMemoryRepository.java
│   │   ├── WarmMemoryRepository.java
│   │   └── RagKnowledgeRepository.java
│   └── service/
│       ├── UserMemoryBootstrapService.java
│       ├── ColdMemoryService.java
│       ├── HotMemoryService.java
│       ├── WarmMemoryRecallService.java
│       ├── RagRecallService.java
│       ├── EmbeddingService.java
│       └── MemoryExtractService.java
└── （改造）app/ParallelLifeApp.java
    （改造）controller/ParallelLifeController.java
    （改造）dto/ChatRequest.java
```

---

## 五、与现有代码的关系

| 现有组件 | MVP 处理方式 |
| --- | --- |
| `FileBasedChatMemory` + kryo | **保留**；`memory.provider=legacy` 时使用 |
| `ParallelLifeVectorStoreConfig` / `SimpleVectorStore` | pg 模式下**不参与**主链路；后续可删或改为「仅向 rag_knowledge 灌数」 |
| `MessageChatMemoryAdvisor` | pg 模式下**移除** |
| `QuestionAnswerAdvisor` | pg 模式下由 `RagRecallService` **替代** |
| `ParallelLifeApp.doChatWithTools` | MVP **不改**；二期 afterRun 同样写 cold |
| `agent/*` Agent 子包 | MVP **不动** |

---

## 六、配置约定（MVP）

```yaml
# application-local.yml（示例结构，密码用环境变量）
memory:
  provider: pg          # pg | legacy
  warm-top-k: 3
  rag-top-k: 3
  cold-recent-rounds: 3
  extract-min-turns: 2

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/agent_memory
    username: ${PG_USER}
    password: ${PG_PASSWORD}
  flyway:
    enabled: true
    locations: classpath:db/migration
```

---

## 七、风险与降级

| 风险 | MVP 应对 |
| --- | --- |
| 本地无 PG | 启动失败并文档指引 Docker 一条命令 |
| embedding 维度与 DDL 不一致 | 第一步锁定维度，Flyway 与 EmbeddingService 同值 |
| 提炼 JSON 解析失败 |  catch + log，不影响用户回复 |
| 向量检索无结果 | 对应 Prompt 块留空，继续生成 |
| 需要快速演示 | `memory.provider=legacy` 一键回滚 |

---

## 八、MVP 完成后 → 扩展路线（非本次范围）

按参考文档「分阶段落地步骤」与仓库已有扩展文档，建议顺序：

1. **流式 + 工具路径**写 cold_memory  
2. **AgentLoop** 接入 `ContextPackage`（见 `AgentLoop编排与三层记忆融合方案.md`）  
3. **Redis** 热层缓存（`三层记忆DDL设计文档` 扩展）  
4. **MySQL** 会话/审计表拆分  
5. 跨会话冷检索、滑动窗口压缩摘要、RAG 模式 2/3/4  
6. Outbox + MQ 一致性  

---

## 九、里程碑与时间（参考）

| 里程碑 | 包含步骤 | 累计预估 |
| --- | --- | --- |
| M1 库就绪 | 第一步 | 0.5～1 天 |
| M2 读写通 | 第二～三步 | 3～4.5 天 |
| M3 能对话 | 第四～五步 | 6.5～9.5 天 |
| M4 闭环 | 第六～七步 | 9.5～14.5 天 |

> 以上为单人全职估算；按「质量优先、小步迭代」可拆成多次 PR，每步合并前需通过该步验收清单。

---

## 十、下一步行动（立即执行）

**当前应执行：第一步 — 基础设施与数据库脚手架**

1. 本地/Docker 启动 PostgreSQL + pgvector  
2. 取消 `pom.xml` 中 JDBC/PostgreSQL/Flyway 注释  
3. 新增 `V1__init_three_layer_memory.sql`  
4. 配置 `application-local.yml` 数据源  
5. 验证 `mvn spring-boot:run` + Flyway 成功  

**第一步通过后**，再进入第二步（Repository + 用户/冷记忆 Service），依此类推。

---

## 十一、相关文档索引

| 文档 | 用途 |
| --- | --- |
| `封装三层记忆模型落地方案(最终标准) .md` | 生产级架构与规则来源 |
| `简化实现方案.md` 第四节 | MVP DDL 权威来源 |
| `三层记忆单库PG-本仓库代码落地方案.md` | 类名与改造文件清单 |
| `简化实现方案-开发任务清单与进度追踪.md` | Phase P0–P9 细项对照 |
| `基础设施与数据库环境准备-任务清单.md` | PG 安装验证 |
| `向量数据库配置指导文档-PostgreSQL与pgvector.md` | 依赖与连接配置 |
| `AgentLoop编排与三层记忆融合方案.md` | MVP 之后编排层融合 |

---

## 附录 A：MVP E2E 演示用例（第七步验收）

**Scenario A — 冷记忆累积**

1. `POST /parallel-life/session` → 拿 `chatId`，传 `userId`  
2. 连续 2 轮 `/parallel-life/chat`  
3. 查 `cold_memory`：`session_id = chatId` 应有 2 行  

**Scenario B — 温记忆沉淀**

1. 在 A 基础上第 3 轮明确用户偏好（如「我偏好稳健型职业路径，不要创业」）  
2. 等待异步提炼（或调低 extract 阈值即时触发）  
3. 新开会话，问「帮我分析职业选择」→ 回答应体现「稳健、不创业」  

**Scenario C — RAG 增强**

1. 确认 `rag_knowledge` 有「人生规划/行业趋势」类 seed  
2. `useRag=true` 提问相关事实性问题  
3. 回答应引用知识库片段（可通过日志看 ragBlock 非空）  
