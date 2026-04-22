# 向量数据库配置指导文档（PostgreSQL + pgvector）

## 1. 文档目标
本指导文档用于帮助项目从当前 `SimpleVectorStore`（内存向量库）迁移到 `PostgreSQL + pgvector`，实现向量数据持久化与可扩展检索。

适用项目：`q-Ai-agent-project`  
当前现状：
1. 已有 RAG 配置类（`ParallelLifeVectorStoreConfig`、`LoveAppVectorStoreConfig`）。
2. 当前使用 `SimpleVectorStore`，应用重启后向量数据会丢失。
3. `pom.xml` 中 pgvector 相关依赖仍处于注释状态。

---

## 2. 为什么选 PostgreSQL + pgvector
1. 与现有后端持久化规划一致（统一数据基础设施）。
2. 支持向量相似度检索（cosine / l2 / inner product）。
3. 便于后续接入会话数据、审计数据和向量数据的一体化治理。
4. 运维复杂度低于独立向量数据库（早期阶段更务实）。

---

## 3. 整体配置步骤（建议顺序）
1. 准备 PostgreSQL 实例并启用 `vector` 扩展。
2. 打开 Maven 依赖（pgvector + jdbc + PostgreSQL 驱动）。
3. 配置 `application-local.yml` 数据源和 Spring AI VectorStore 参数。
4. 准备数据库 DDL（向量表 + 索引）。
5. 改造 RAG 配置：`SimpleVectorStore` -> `PgVectorStore`。
6. 验证：建库、写入文档向量、相似检索回归。

---

## 4. 环境准备

## 4.1 PostgreSQL 版本建议
- PostgreSQL `15+`（建议）
- `pgvector` 扩展可用

## 4.2 下载清单（必须/可选）
为了把向量数据库跑起来，建议先准备以下软件：

### 必须下载
1. **PostgreSQL（15+）**
   - 下载入口：[PostgreSQL Downloads](https://www.postgresql.org/download/)
   - 说明：如果你不用 Docker，需要本机安装 PostgreSQL 服务端。
2. **pgvector 扩展**
   - 项目地址：[pgvector GitHub](https://github.com/pgvector/pgvector)
   - 说明：若使用 `pgvector/pgvector` Docker 镜像通常已内置；本机安装 PostgreSQL 时需确认可安装此扩展。

### 推荐下载（二选一）
1. **Docker Desktop（推荐）**
   - 下载入口：[Docker Desktop](https://www.docker.com/products/docker-desktop/)
   - 说明：可直接拉取 `pgvector/pgvector` 镜像，最快完成本地环境搭建。
2. **本机 PostgreSQL 安装包**
   - 适合不使用容器的开发环境。

### 可选工具（提升效率）
1. **DBeaver**（可视化管理数据库）
   - 下载入口：[DBeaver Community](https://dbeaver.io/download/)
2. **pgAdmin**（PostgreSQL 官方管理工具）
   - 下载入口：[pgAdmin Download](https://www.pgadmin.org/download/)
3. **Flyway CLI**（做数据库迁移可选）
   - 下载入口：[Flyway](https://documentation.red-gate.com/fd/command-line-184127404.html)

## 4.2 本地快速启动（Docker 示例）
```bash
docker run -d \
  --name pgvector-db \
  -e POSTGRES_DB=q_ai_agent \
  -e POSTGRES_USER=my_user \
  -e POSTGRES_PASSWORD=your_password \
  -p 5432:5432 \
  pgvector/pgvector:pg16
```

启动后执行：
```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

---

## 4.3 Maven 依赖（由项目自动下载）
这部分不需要手工下载 jar，Maven 会在构建时自动拉取：
1. `spring-ai-starter-vector-store-pgvector`
2. `spring-boot-starter-jdbc`
3. `postgresql`

如果拉取失败，可执行：
```bash
mvn -U clean compile
```
并检查网络与 Maven 镜像配置。

---

## 5. Maven 依赖配置（项目内）
在 `pom.xml` 中启用以下依赖（把原注释块改为真实依赖）：

1. `spring-ai-starter-vector-store-pgvector`
2. `spring-boot-starter-jdbc`
3. `postgresql`（runtime）

说明：
- 优先保持 Spring AI 相关依赖版本一致（避免 M6/M7 混用）。
- 你当前项目已使用 Spring AI Alibaba `1.0.0-M6.1`，建议先选与其兼容的 pgvector 组件版本。

---

## 6. 配置文件示例（application-local.yml）
以下为本地 profile 推荐配置（示例，按你的环境调整）：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/q_ai_agent
    username: my_user
    password: your_password

  ai:
    vectorstore:
      pgvector:
        initialize-schema: true
        dimensions: 1536
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        max-document-batch-size: 1000
```

参数说明：
1. `dimensions` 必须与 Embedding 模型输出维度一致（错误会导致写入失败）。
2. `distance-type` 推荐优先用 `COSINE_DISTANCE`。
3. `initialize-schema` 在开发环境可开启，生产建议改由迁移脚本管理。

---

## 7. 数据库表与索引建议（向量专项）

> 如果后续采用 Spring AI 自动建表，可先验证功能；生产建议切换到手工 DDL + Flyway 管控。

建议基础表（示例）：
```sql
CREATE TABLE IF NOT EXISTS rag_document_embedding (
  id BIGSERIAL PRIMARY KEY,
  doc_id VARCHAR(64) NOT NULL,
  source VARCHAR(64),
  content TEXT NOT NULL,
  metadata JSONB,
  embedding VECTOR(1536) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_rag_doc_source
  ON rag_document_embedding(source);
```

向量索引（根据数据规模选择）：
```sql
-- HNSW 示例（大规模检索建议）
CREATE INDEX IF NOT EXISTS idx_rag_doc_embedding_hnsw
ON rag_document_embedding
USING hnsw (embedding vector_cosine_ops);
```

---

## 8. 代码改造指引（从 SimpleVectorStore 迁移）

当前类：
- `rag/ParallelLifeVectorStoreConfig`
- `rag/LoveAppVectorStoreConfig`

改造方向：
1. 将 `SimpleVectorStore.builder(...)` 替换为 `PgVectorStore`（由 Spring 管理 Bean）。
2. 文档加载流程保持不变：仍使用 `DocumentLoader`，但落库到 pgvector。
3. 首次启动可执行“初始化导入”；后续改为增量更新策略（避免重复写）。

建议策略：
1. 首期先改一个场景（如 `parallelLifeVectorStore`）试点。
2. 验证稳定后再迁移另一个场景（`loveAppVectorStore`）。

---

## 9. 验证清单（上线前必须过）
1. 应用启动成功，数据源连接正常。
2. `vector` 扩展可用。
3. 能写入至少 10 条文档向量。
4. 相同 query 连续检索结果稳定。
5. 重启应用后向量数据仍可检索（验证持久化价值）。
6. 检索延迟在可接受范围（记录 P50/P95）。

---

## 10. 常见问题与排查
1. **报错：dimension mismatch**
   - 原因：Embedding 输出维度和表定义 `VECTOR(n)` 不一致。
   - 处理：统一模型与 `dimensions` 配置。

2. **报错：type "vector" does not exist**
   - 原因：未执行 `CREATE EXTENSION vector`。
   - 处理：在目标数据库启用扩展。

3. **检索慢**
   - 原因：无向量索引或表数据膨胀。
   - 处理：增加 HNSW/IVFFlat 索引，控制 chunk 粒度与冗余文档。

4. **重复写入严重**
   - 原因：每次启动重复全量导入。
   - 处理：增加文档哈希去重与增量同步。

---

## 11. 分阶段实施建议（与你当前计划对齐）
1. **阶段 A（准备）**
   - 开依赖、配数据源、启扩展、跑通最小写入与检索。
2. **阶段 B（试点）**
   - 仅迁移一个向量库配置类，做功能与性能回归。
3. **阶段 C（推广）**
   - 迁移第二个配置类，统一索引与监控。
4. **阶段 D（治理）**
   - 引入 Flyway 管控 DDL、增量更新、定期清理策略。

---

## 12. 与现有文档关系
1. 与 `docs-back/数据持久化前置设计-数据库表结构方案.md` 互补：
   - 该文档偏“业务持久化表设计”
   - 本文档偏“向量库配置与落地操作”
2. 与 `docs-back/RAG设计与优化.md` 联动：
   - 后续检索策略优化可在 RAG 文档中深化

