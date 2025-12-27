# 平行宇宙人生模拟器 (Parallel Life Simulator)

<div align="center">

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.8-brightgreen)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0--M6.1-blue)
![License](https://img.shields.io/badge/license-MIT-green)

**通过AI模拟不同人生路径，帮助用户探索人生可能性、理解决策影响、获得人生规划建议**

[功能特性](#-功能特性) • [快速开始](#-快速开始) • [API文档](#-api文档) • [项目结构](#-项目结构) • [开发指南](#-开发指南)

</div>

---

## 📖 项目简介

平行宇宙人生模拟器是一个基于 Spring AI 和阿里云百炼大模型的智能人生规划助手。通过AI技术模拟多个平行宇宙的人生发展路径，帮助用户：

- 🎯 **探索人生可能性**：模拟不同选择可能带来的结果
- 📊 **理解决策影响**：分析决策在多个平行宇宙中的影响
- 💡 **获得专业建议**：基于知识库和数据分析提供人生规划建议
- 📈 **评估成功概率**：评估各种人生路径的成功概率和风险

## ✨ 功能特性

### 核心功能

- 🤖 **智能对话**：基于Spring AI的对话系统，支持多轮对话和上下文理解
- 🔄 **流式响应**：支持Server-Sent Events (SSE)流式输出，实时显示AI回复
- 📚 **RAG增强**：结合知识库（职业发展、决策分析、人生规划）提供专业建议
- 🛠️ **工具调用**：支持网络搜索、文件操作、PDF生成等多种工具
- 💾 **持久化记忆**：基于文件的对话记忆，支持多会话管理
- 📊 **结构化报告**：生成包含多个平行宇宙的详细分析报告

### 技术亮点

- ✅ **流式API**：实时返回AI回复，提升用户体验
- ✅ **统一异常处理**：全局异常处理器，优雅的错误处理
- ✅ **参数校验**：使用Bean Validation进行参数校验
- ✅ **Swagger文档**：自动生成API文档，支持在线测试
- ✅ **违禁词过滤**：内置内容安全过滤机制

## 🛠️ 技术栈

### 核心框架
- **Spring Boot 3.4.8** - 应用框架
- **Spring AI Alibaba 1.0.0-M6.1** - AI框架集成
- **Java 21** - 编程语言

### AI相关
- **阿里云百炼（通义千问）** - 大语言模型
- **Spring AI** - AI应用开发框架
- **SimpleVectorStore** - 向量存储（内存）

### 工具库
- **Hutool** - Java工具类库
- **Kryo** - 序列化库（对话记忆持久化）
- **iText** - PDF生成
- **Jsoup** - 网页解析
- **Knife4j** - API文档增强

### 开发工具
- **Maven** - 项目构建
- **Lombok** - 代码简化
- **Swagger/OpenAPI** - API文档

## 🚀 快速开始

### 环境要求

- JDK 21+
- Maven 3.6+
- 阿里云百炼API Key

### 安装步骤

1. **克隆项目**
```bash
git clone <repository-url>
cd q-Ai-agent-project
```

2. **配置API密钥**

创建 `src/main/resources/application-local.yml` 文件（如果不存在）：

```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}  # 建议使用环境变量
      chat:
        options:
          model: qwen-plus
```

或使用环境变量：
```bash
export DASHSCOPE_API_KEY=your-api-key-here
```

3. **编译项目**
```bash
mvn clean install
```

4. **运行项目**
```bash
mvn spring-boot:run
```

或直接运行jar包：
```bash
java -jar target/q-Ai-agent-project-0.0.1-SNAPSHOT.jar
```

5. **访问应用**

- 应用地址：http://localhost:8123/api
- API文档：http://localhost:8123/api/swagger-ui.html
- 健康检查：http://localhost:8123/api/health

## 📁 项目结构

```
q-Ai-agent-project/
├── docs/                          # 项目文档
│   ├── API接口文档.md            # API接口详细文档
│   ├── 业务流程文档.md            # 业务流程说明
│   ├── 后端改进计划.md            # 改进计划
│   └── 平行宇宙人生模拟器-详细设计.md
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/qin/qaiagentproject/
│   │   │       ├── advisor/      # Advisor拦截器
│   │   │       ├── agent/        # AI代理（qManus）
│   │   │       ├── app/          # 核心应用（ParallelLifeApp）
│   │   │       ├── chatmeomery/  # 对话记忆
│   │   │       ├── common/       # 通用类（Result等）
│   │   │       ├── constant/    # 常量定义
│   │   │       ├── controller/   # REST控制器
│   │   │       ├── dto/          # 数据传输对象
│   │   │       ├── exception/    # 异常处理
│   │   │       ├── rag/          # RAG相关（向量存储、文档加载）
│   │   │       └── tools/        # AI工具（文件、搜索、PDF等）
│   │   └── resources/
│   │       ├── application.yml  # 应用配置
│   │       ├── application-local.yml  # 本地配置
│   │       └── document/        # 知识库文档（Markdown）
│   └── test/                     # 测试代码
├── tmp/                          # 临时文件（对话记忆、下载文件等）
├── pom.xml                       # Maven配置
└── README.md                     # 项目说明
```

## ⚙️ 配置说明

### 应用配置

主要配置在 `src/main/resources/application.yml`：

```yaml
spring:
  application:
    name: q-AI-agent-Project-Backend
  profiles:
    active: local

server:
  port: 8123
  servlet:
    context-path: /api

search-api:
  api-key: ${SEARCH_API_KEY}  # SearchAPI密钥（可选）

springdoc:
  swagger-ui:
    path: /swagger-ui.html
```

### 环境变量

建议使用环境变量管理敏感信息：

```bash
# 阿里云百炼API Key（必需）
export DASHSCOPE_API_KEY=your-api-key

# SearchAPI密钥（可选，用于网络搜索功能）
export SEARCH_API_KEY=your-search-api-key
```

### 文件存储路径

- 对话记忆：`{user.dir}/tmp/chat-memory/`
- 文件操作：`{user.dir}/tmp/file/`
- 资源下载：`{user.dir}/tmp/download/`
- PDF生成：`{user.dir}/tmp/pdf/`

## 📡 API文档

### 快速体验

#### 1. 创建会话
```bash
curl -X POST http://localhost:8123/api/parallel-life/session \
  -H "Content-Type: application/json" \
  -d '{"userId": "user123", "sessionName": "我的职业规划"}'
```

#### 2. 流式对话（推荐）
```bash
curl -X POST http://localhost:8123/api/parallel-life/chat/stream \
  -H "Content-Type: application/json" \
  -d '{
    "message": "我今年25岁，是一名程序员，工作2年了",
    "chatId": "your-chat-id",
    "useRag": true
  }'
```

#### 3. 生成报告
```bash
curl -X POST http://localhost:8123/api/parallel-life/report \
  -H "Content-Type: application/json" \
  -d '{
    "message": "我考虑是否应该转行做产品经理",
    "chatId": "your-chat-id"
  }'
```

### 前端集成示例

#### JavaScript (Fetch API)
```javascript
// 流式对话
async function streamChat(message, chatId) {
  const response = await fetch('http://localhost:8123/api/parallel-life/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message, chatId })
  });

  const reader = response.body.getReader();
  const decoder = new TextDecoder();

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    
    const chunk = decoder.decode(value);
    console.log(chunk);  // 实时显示AI回复
  }
}
```

### 完整API文档

详细API文档请访问：
- **Swagger UI**: http://localhost:8123/api/swagger-ui.html
- **API文档**: [docs/API接口文档.md](docs-back/API接口文档.md)

## 🎯 核心功能说明

### 1. 平行人生模拟

系统会基于用户当前情况，模拟3-5个不同的平行宇宙路径，每个宇宙包含：
- 📅 **时间线**：1年、3年、5年、10年的发展节点
- 🎯 **关键事件**：重要转折点和机遇
- 📊 **人生指标**：幸福感、财务状况、成长潜力等（1-100分）
- 📈 **实现概率**：基于客观因素评估的成功概率

### 2. 决策影响分析

分析特定决策在多个平行宇宙中的影响：
- ✅ **成功路径**：决策成功后的发展
- ⚠️ **风险路径**：决策失败或遇到挑战的情况
- 🔄 **折中路径**：部分成功的情况

### 3. RAG增强

结合知识库提供专业建议：
- 📚 **职业发展路径**：不同职业的发展轨迹
- 📖 **决策分析方法论**：科学的决策分析框架
- 🎓 **人生规划方法论**：系统的人生规划方法

### 4. 工具调用

AI可以调用多种工具完成任务：
- 🔍 **网络搜索**：搜索行业数据、薪资水平等
- 📄 **文件操作**：保存报告、读取文件
- 📥 **资源下载**：下载图片、文档等
- 📑 **PDF生成**：生成精美的PDF报告

## 🧪 测试

### 运行测试
```bash
mvn test
```

### 测试覆盖
- 单元测试：`src/test/java/com/qin/qaiagentproject/`
- 集成测试：包含API测试和功能测试

主要测试类：
- `ParallelLifeAppTest` - 核心功能测试
- `YuManusTest` - AI代理测试

## 📚 文档

- [API接口文档](docs-back/API接口文档.md) - 完整的API使用说明
- [业务流程文档](docs-back/业务流程文档.md) - 系统架构和业务流程
- [后端改进计划](docs-back/后端改进计划.md) - 改进计划和路线图
- [详细设计文档](docs-back/平行宇宙人生模拟器-详细设计.md) - 产品设计文档

## 🔧 开发指南

### 代码规范

- 使用 Lombok 简化代码
- 遵循 Spring Boot 最佳实践
- 统一异常处理（GlobalExceptionHandler）
- 统一响应格式（Result类）

### 添加新功能

1. **添加新工具**
   - 在 `tools/` 目录创建工具类
   - 使用 `@Tool` 注解标记方法
   - 在 `ToolRegistration` 中注册

2. **添加新Advisor**
   - 实现 `CallAroundAdvisor` 或 `StreamAroundAdvisor`
   - 在 `ParallelLifeApp` 中配置

3. **添加新API**
   - 在 `controller/` 目录创建Controller
   - 使用 `@RestController` 和 `@RequestMapping`
   - 添加 Swagger 注解

### 常见问题

**Q: 如何修改系统提示词？**  
A: 修改 `ParallelLifeApp.java` 中的 `SYSTEM_PROMPT` 常量

**Q: 如何添加新的知识库文档？**  
A: 将Markdown文件放入 `src/main/resources/document/` 目录，系统会自动加载

**Q: 如何配置向量存储？**  
A: 修改 `ParallelLifeVectorStoreConfig.java`，可以切换到PostgreSQL+pgvector

## 🚧 已知问题与改进计划

### 当前限制

- ⚠️ 向量存储使用内存，服务重启后需重新加载
- ⚠️ 会话信息未完全持久化
- ⚠️ 缺少用户认证和权限控制
- ⚠️ 缺少API限流机制

### 改进计划

详细的改进计划请查看：[后端改进计划](docs-back/后端改进计划.md)

**优先级排序**：
1. 🔴 **P0（紧急）**：API密钥管理、工具安全控制、数据持久化
2. 🟡 **P1（重要）**：服务层重构、异常处理、监控告警
3. 🟢 **P2（一般）**：代码清理、缓存机制、测试覆盖

## 🤝 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📝 更新日志

### v0.0.1-SNAPSHOT (2025-01-31)

- ✨ 实现平行宇宙人生模拟器核心功能
- ✨ 支持流式对话API
- ✨ 集成RAG增强功能
- ✨ 实现工具调用机制
- ✨ 添加Swagger API文档
- 📚 完善项目文档

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 👥 作者

- 秦健超

## 🙏 致谢

- [Spring AI](https://spring.io/projects/spring-ai) - AI应用开发框架
- [阿里云百炼](https://dashscope.aliyuncs.com/) - 大语言模型服务
- [Knife4j](https://doc.xiaominfo.com/) - API文档增强工具

## 📞 联系方式

如有问题或建议，请通过以下方式联系：

- 提交 Issue
- 发送邮件 q110176418@qq.com

---

<div align="center">

**⭐ 如果这个项目对你有帮助，请给个Star支持一下！⭐**

Made with ❤️ by the development team

</div>

