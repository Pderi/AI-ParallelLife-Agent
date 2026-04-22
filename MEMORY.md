# MEMORY.md

## 项目速览
- 项目名：`q-Ai-agent-project`
- 后端技术栈：`Spring Boot 3`、`Spring AI Alibaba`、`Java 21`、`Maven`
- 核心定位：Agent + 工具调用 + RAG（平行宇宙人生模拟器场景）
- 当前目标：按 `docs-back/后端改进计划.md` 逐步完成后端工程化改造（质量优先，小步迭代）

## 目录索引（高频）
- 后端代码：`src/main/java/com/qin/qaiagentproject`
- 配置文件：`src/main/resources`
- 测试代码：`src/test/java/com/qin/qaiagentproject`
- 后端文档：`docs-back`
- 前端文档：`docs-front`
- AI 规则：`.cursor/rules`

## 当前架构关键点
- 控制器入口：`ParallelLifeController`
- Agent 基类：`agent/BaseAgent`
- 工具注册：`tools/ToolRegistration`
- 工具实现：`tools/*Tool.java`
- 全局异常处理：`exception/GlobalExceptionHandler`
- 统一响应：`common/Result`

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

## 当前待办（高优先）
1. 数据持久化最小闭环（P0）
2. 引入服务层（P1）
3. 请求限流（P1）
4. 监控与告警（P1）

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
- 三层记忆DDL设计文档：`docs-back/三层记忆DDL设计文档-Redis+PostgreSQL+MySQL.md`
- 消息一致性保障方案：`docs-back/消息一致性保障方案-MySQL主链路与三库协同.md`
- 消息队列选型与最小接入方案：`docs-back/消息队列选型与最小接入方案-三层记忆架构.md`

## 文档索引（前端）
- 前端初始化提示词：`docs-front/前端初始化提示词.md`
- 核心页面交互优化提示词：`docs-front/核心功能页面展示与交互设计优化提示词.md`

## 使用约定
1. 开始任何新改动前，先阅读本文件与 `docs-back/后端改进计划.md`。
2. 每次完成改动后，更新“最近已完成改造（摘要）”与“当前待办（高优先）”。
3. 仅记录已验证通过（可编译/可测试/可复现）的事实，不记录未落地设想。
