# MEMORY.md

## 项目速览
- 项目名：`q-Ai-agent-project`
- 后端技术栈：`Spring Boot 3`、`Spring AI Alibaba`、`Java 21`、`Maven`
- 核心定位：Agent + 工具调用 + RAG（平行宇宙人生模拟器场景）
- **展示定位**：秋招简历用个人项目——优先「可演示、可讲清、与岗位栈一致」，技术复杂度与投递岗位（Java 后端 / AI 应用）对齐后再逐步加码。
- **持久化策略（已定）**：首期以 **PostgreSQL（含 pgvector）单库** 跑通三层记忆 + RAG 全流程闭环；表结构以 `docs-back/简化实现方案.md` 第四节为**首期参考**。**MySQL + Redis + PostgreSQL 三库分工**、对应 DDL 与一致性/MQ 文档保留为**扩展演进**，待单库闭环稳定后再按需落地。
- 当前目标：按 `docs-back/后端改进计划.md` 逐步完成后端工程化改造（质量优先，小步迭代）

## 目录索引（高频）
- 纠错日志：`ERROR.md`
- 后端代码：`src/main/java/com/qin/qaiagentproject`
- 配置文件：`src/main/resources`
- 测试代码：`src/test/java/com/qin/qaiagentproject`
- 后端文档：`docs-back`
- 前端文档：`docs-front`
- AI 规则：`.cursor/rules`（三层记忆 + 纠错见 `three-layer-memory-error-workflow.mdc`；稳定生成见 `prompt-optimize-before-commands.mdc`、`code-quality-over-velocity.mdc`、`implementation-plan-before-code.mdc`）

## 当前架构关键点
- 控制器入口：`ParallelLifeController`
- Agent 基类：`agent/BaseAgent`
- 工具注册：`tools/ToolRegistration`
- 工具实现：`tools/*Tool.java`
- 全局异常处理：`exception/GlobalExceptionHandler`
- 统一响应：`common/Result`
- 三层记忆（pg）：`context/ContextManager`、`memory/service/*`、`memory/repository/*`
- 记忆开关：`memory.provider=pg|legacy`（`config/MemoryProperties`）

## 最近已完成改造（摘要）
1. 安全加固（首批）
   - `search-api` 主配置明文移除，local 配置管理。
   - `TerminalOperationTool` 增加默认禁用、白名单、危险字符拦截。
   - `FileOperationTool` 增加文件名白名单与路径穿越防护。
2. 异常体系统一（第一阶段）
   - 新增 `ErrorCode`、`BusinessException`。
   - `GlobalExceptionHandler` 统一错误响应。
   - `FileOperationTool`、`TerminalOperationTool`、`WebSearchTool` 已迁移到统一异常语义。
3. 质量配套
   - 新增规则：质量优先、减少冗余 try-catch。
   - 新增测试：`FileOperationToolTest`、`TerminalOperationToolTest`。
   - 清理日志冲突：排除 `slf4j-simple`。
4. 数据持久化前置准备
   - 已输出数据库表结构设计文档（会话、消息、工具审计 + DDL 草案）。
5. 向量库配置准备
   - 已输出 PostgreSQL + pgvector 配置指导文档（依赖、配置、DDL、迁移、排查与下载清单）。
6. 上下文改造落地方案
   - 已输出三层记忆 + 自愈机制在当前代码中的分阶段落地文档（含模块拆分、与持久化对接、验收指标）。
7. 三层记忆项目化落地方案
   - 已输出结合现有代码与数据持久化路径的完整实施文档（单库起步、分阶段演进、可回滚方案）。
8. 三层记忆终极落地方案
   - 已输出参考生产级蓝图并适配项目现状的终极实施方案（架构、链路、迁移、降级、回滚、验收标准）。
9. 三层记忆终极方案补充
   - 已按新决策补充首期引入 Redis + PostgreSQL + MySQL，并新增用户表与租户隔离设计。
10. 三层记忆DDL文档
   - 已新增 Redis + PostgreSQL + MySQL 的独立 DDL 设计文档，含建表SQL、Redis key规范与写入顺序。
11. 前置持久化文档对齐说明
   - 已在前置表结构文档顶部补充“以三层记忆DDL文档为准”的替代提示，避免并行引用造成混乱。
12. 三库表归属调整
   - 已按最新决策将 `chat_session`、`chat_message`、`tool_call_audit` 统一迁移至 MySQL，PostgreSQL 聚焦 Warm/RAG。
13. 消息一致性保障方案
   - 已新增 MySQL 主链路 + Redis/PG 协同的一致性文档，覆盖 Outbox、幂等、重试、死信、对账修复。
14. 消息队列选型方案
   - 已新增 MQ 选型与最小接入文档，给出 RabbitMQ 优先建议与 Outbox+MQ 演进路径。
15. 参考与对照文档
   - `docs-back/简化实现方案.md`：秋招向「极简三层记忆+RAG」说明；**首期单库 PG 表设计以此文档第四节为主参考**（Java 实现与之对齐）。三库 DDL 文档见扩展项。
   - `docs-back/封装三层记忆模型落地方案 .md` 已加入：生产级三层记忆+RAG 长文参考（注意文件名在扩展名前有单个空格）。
16. Cursor 规则
   - 已新增 `.cursor/rules/three-layer-memory-error-workflow.mdc`：热记忆=`MEMORY.md`、温记忆=`docs-front`/`docs-back`、冷记忆=代码库；纠错=`ERROR.md`；生成前后必读/必更。
   - `error-read-and-log.mdc` 已改为读取项目根目录 `ERROR.md`（不再使用外部绝对路径）。
17. 简化实现方案落地
   - 已输出基于 `简化实现方案.md` 的 Java 工程映射版开发任务清单与进度追踪文档（Phase P0–P9、里程碑、风险表）。
18. 基础设施环境准备清单
   - 已输出 MySQL / PostgreSQL（pgvector）/ Redis 安装配置与 DDL 验证的进度追踪文档（Phase A–G）。
20. 任务清单与基础设施清单对齐单 PG
   - 已更新 `简化实现方案-开发任务清单与进度追踪.md`：首期单 PostgreSQL + 简化方案第四节 DDL；P1–P9 与扩展 Phase X1–X3（三库/MQ）。
   - 已更新 `基础设施与数据库环境准备-任务清单.md`：首期以 Phase C（PG）为主，MySQL/Redis 标为扩展可跳过。
21. 本仓库代码落地方案
   - 已输出 `三层记忆单库PG-本仓库代码落地方案.md`：结合现有包结构列出需改文件（`ParallelLifeApp`、`ParallelLifeController`、`ChatRequest` 等）、建议新增包（`context/`、`memory/`）、`pom`/Flyway 与 RAG 统一策略。
19. 持久化路径收敛（2026）
   - 确认：单 PostgreSQL 为个人项目首期实现与简历叙事主线；三库方案仅作扩展，不阻塞闭环开发。
22. AgentLoop 与三层记忆融合文档
   - 已输出 `docs-back/AgentLoop编排与三层记忆融合方案.md`：`ContextManager` 单一拼装入口、冷写与异步提炼、`ContextPackage` 与 `ParallelLifeAgentLoop` 契约、pg/legacy 约定及验收清单。
23. Cursor 规则（稳定生成）
   - 新增 `prompt-optimize-before-commands.mdc`（执行命令前优化/重述意图）、`code-quality-over-velocity.mdc`（质量优于速度、任务拆分与 `quality-first-incremental-delivery` 配合）、`implementation-plan-before-code.mdc`（先方案、用户确认后再写实现代码）。
24. 实验报告（当前代码基线）
   - 已输出 `docs-back/平行宇宙人生模拟器-实验报告.md`：v2.0 课程设计汇报体例（摘要/需求/设计/实现含关键代码/测试/总结/三人体会）；工具安全单测通过；分工不写入报告。
25. 前端视觉（2026-06）
   - 去 AI 味：移除 `#667eea`/`#764ba2` 蓝紫渐变；统一 `variables.css` 设计变量。
   - 当前主题：暖纸质感（`#f4f1eb` 背景 + 铜赭 `#b8734a` 主色 + 墨绿灰辅色）；深色模式为暖色深灰；宇宙卡片为大地/墨绿/赭石系。
26. 三层记忆 MVP 分步开发计划
   - 已输出 `docs-back/07-计划与任务/三层记忆MVP分步开发计划.md`：参考「封装三层记忆模型落地方案(最终标准)」收敛为单 PG MVP，七步执行顺序（库脚手架 → Repository → 向量检索 → ContextManager → 主链路 → 异步提炼 → E2E）；第一步为 Flyway + 数据源。
27. 简历描述（完成态 v2.3）
   - `docs-back/后端项目简历描述.md`：完成态叙事 + 岗位裁剪/量化/追问；RAG 区分「简历可贴约 2 行」与「面试四段展开」，避免长文误贴简历。
28. 三层记忆 MVP 代码落地（2026-08，未验库）
   - 依赖：`jdbc` + `postgresql` + `flyway`；`V1__init_three_layer_memory.sql`（五表 + vector 1536）。
   - 包：`memory/repository|service`、`context/*`、`config/MemoryProperties|AsyncConfig|PgMemoryDataSourceConfig`。
   - 主链路：`ParallelLifeApp` / `ParallelLifeController` / `ChatRequest.userId`；`memory.provider=pg|legacy`。
   - pg：`ContextManager` 并行召回 → 写 `cold_memory` → `@Async` 冷→温→热；RAG 走 `rag_knowledge`；启动空表灌种子。
   - 默认 `memory.provider=legacy`；示例配置见 `application-local.yml.example`。
   - 已编译通过；`PromptAssemblerTest` 通过；**尚未验证 PostgreSQL 连接 / Flyway 实库迁移**。
29. 三层记忆 MVP 实现说明文档
   - 已输出 `docs-back/03-三层记忆/三层记忆MVP-代码实现与全链路说明.md`：包结构、类职责、全链路时序、双模式、API 示例与验收对照。
   - 已补充 Mermaid 流程图：表层级、主链路 flowchart/sequence、并行召回、冷→温→热、pg/legacy 分流。

## 当前待办（高优先）
1. **联调验收**：本地 PG + pgvector，按 `application-local.yml.example` 切 `memory.provider=pg`，跑通 Scenario A/B/C（见 MVP 分步计划附录）。
2. **自研 AgentLoop（编排层）**：在 `orchestration/` 新增请求级多步 ReAct + 可选规划循环，`ParallelLifeApp` 提供 `doChatWithAgent`（或与工具路径合并）；与 `ChatClient` 记忆/RAG 对齐策略见设计讨论（首期可「循环前后读写 ChatMemory」+ 首轮手动 RAG 注入）。
3. 引入服务层（P1）
4. 请求限流（P1）
5. 监控与告警（P1）

## 文档索引（后端）
- 改进总计划：`docs-back/后端改进计划.md`
- Agent 高并发方案：`docs-back/大厂级Agent改进计划-高并发内生化.md`
- 上下文管理方案：`docs-back/上下文管理改造方案-三层记忆与自愈机制.md`
- RAG 设计：`docs-back/RAG设计与优化.md`
- API 文档：`docs-back/API接口文档.md`
- 业务流程：`docs-back/业务流程文档.md`
- 详细设计：`docs-back/平行宇宙人生模拟器-详细设计.md`
- 项目简历描述：`docs-back/后端项目简历描述.md`
- 面试问答：`docs-back/面试问答文档.md`
- 数据库表设计（持久化前置）：`docs-back/数据持久化前置设计-数据库表结构方案.md`
- 向量数据库配置指导：`docs-back/向量数据库配置指导文档-PostgreSQL与pgvector.md`
- 上下文改造代码落地方案：`docs-back/上下文管理改造代码落地方案.md`
- 三层记忆项目化落地方案：`docs-back/三层记忆落地方案-基于现有项目与持久化路径.md`
- 三层记忆终极落地方案：`docs-back/三层记忆终极落地方案-项目实战版.md`
- 三层记忆DDL设计文档（**扩展**：MySQL+PG+Redis）：`docs-back/三层记忆DDL设计文档-Redis+PostgreSQL+MySQL.md`
- 消息一致性保障方案：`docs-back/消息一致性保障方案-MySQL主链路与三库协同.md`
- 消息队列选型与最小接入方案：`docs-back/消息队列选型与最小接入方案-三层记忆架构.md`
- 极简实现参考（秋招/演示向）：`docs-back/简化实现方案.md`
- 简化实现方案 — 开发任务清单与进度追踪：`docs-back/简化实现方案-开发任务清单与进度追踪.md`
- 三层记忆单库 PG — 本仓库代码落地方案：`docs-back/三层记忆单库PG-本仓库代码落地方案.md`
- AgentLoop 编排与三层记忆融合：`docs-back/AgentLoop编排与三层记忆融合方案.md`
- 实验报告（当前代码基线）：`docs-back/平行宇宙人生模拟器-实验报告.md`
- 基础设施与数据库环境准备（安装/配置/验证）：`docs-back/基础设施与数据库环境准备-任务清单.md`
- 封装三层记忆模型（生产级参考，文件名含空格）：`docs-back/03-三层记忆/封装三层记忆模型落地方案(最终标准) .md`
- 三层记忆 MVP 分步开发计划：`docs-back/07-计划与任务/三层记忆MVP分步开发计划.md`
- **三层记忆 MVP 代码实现与全链路说明（读代码优先）**：`docs-back/03-三层记忆/三层记忆MVP-代码实现与全链路说明.md`

## 文档索引（前端）
- 前端初始化提示词：`docs-front/前端初始化提示词.md`
- 核心页面交互优化提示词：`docs-front/核心功能页面展示与交互设计优化提示词.md`

## 使用约定
1. 开始任何新改动前，先阅读本文件与 `docs-back/后端改进计划.md`。
2. 每次完成改动后，更新“最近已完成改造（摘要）”与“当前待办（高优先）”。
3. 仅记录已验证通过（可编译/可测试/可复现）的事实，不记录未落地设想。
