# 平行宇宙人生模拟器 API 接口文档

## 基础信息

- **Base URL**: `http://localhost:8123/api`
- **API文档**: `http://localhost:8123/api/swagger-ui.html`
- **Content-Type**: `application/json`

## 统一响应格式

所有接口都使用统一的响应格式：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1704067200000
}
```

### 响应码说明

- `200`: 操作成功
- `400`: 参数错误
- `500`: 服务器错误

## API 接口列表

### 1. 创建会话

**接口地址**: `POST /parallel-life/session`

**接口描述**: 创建一个新的对话会话，返回会话ID

**请求参数**:

```json
{
  "userId": "user123",        // 可选，用户ID
  "sessionName": "我的职业规划"  // 可选，会话名称
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "会话创建成功",
  "data": {
    "chatId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "user123",
    "sessionName": "我的职业规划",
    "createTime": "2025-01-31T10:00:00",
    "updateTime": "2025-01-31T10:00:00"
  },
  "timestamp": 1704067200000
}
```

### 2. 流式对话（推荐）

**接口地址**: `POST /parallel-life/chat/stream`

**接口描述**: 与AI进行流式对话，实时返回AI回复内容（Server-Sent Events）

**Content-Type**: `text/event-stream`

**请求参数**:

```json
{
  "message": "我今年25岁，是一名程序员，工作2年了",
  "chatId": "550e8400-e29b-41d4-a716-446655440000",
  "useRag": false,      // 可选，是否使用RAG增强，默认false
  "useTools": false     // 可选，是否使用工具调用，默认false
}
```

**参数说明**:
- `message`: 必填，用户消息内容
- `chatId`: 必填，会话ID
- `useRag`: 可选，是否使用RAG增强（结合知识库），默认false
- `useTools`: 可选，是否使用工具调用（搜索、PDF生成等），默认false

**响应格式**: Server-Sent Events (SSE)

**响应示例**:
```
data: 欢迎来到平行宇宙人生模拟器！

data: 我是你的专属人生规划师。

data: 告诉我你当前的情况或想要探索的人生选择...
```

**前端调用示例（JavaScript）**:
```javascript
const eventSource = new EventSource('http://localhost:8123/api/parallel-life/chat/stream', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    message: '我今年25岁，是一名程序员',
    chatId: 'xxx',
    useRag: false
  })
});

eventSource.onmessage = (event) => {
  const content = event.data;
  // 实时显示AI回复内容
  console.log(content);
};
```

**使用 Fetch API 的流式调用**:
```javascript
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
    // 处理流式数据
    console.log(chunk);
  }
}
```

### 2.1 基础对话（非流式，兼容旧版本）

**接口地址**: `POST /parallel-life/chat`

**接口描述**: 与AI进行对话，等待完整回复后返回（推荐使用流式接口）

**请求参数**: 同流式接口

**响应示例**:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "content": "欢迎来到平行宇宙人生模拟器！我是你的专属人生规划师...",
    "chatId": "550e8400-e29b-41d4-a716-446655440000"
  },
  "timestamp": 1704067200000
}
```

### 3. 快速流式对话（推荐）

**接口地址**: `POST /parallel-life/quick-chat/stream`

**接口描述**: 快速流式对话接口，无需提供chatId，系统自动创建会话

**Content-Type**: `text/event-stream`

**请求参数**:

```json
"我今年25岁，是一名程序员，工作2年了"
```

**说明**: 直接发送消息字符串，系统会自动创建会话并流式返回结果

**响应格式**: Server-Sent Events (SSE)

**前端调用示例**:
```javascript
async function quickStreamChat(message) {
  const response = await fetch('http://localhost:8123/api/parallel-life/quick-chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(message)
  });

  const reader = response.body.getReader();
  const decoder = new TextDecoder();

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    
    const chunk = decoder.decode(value);
    console.log(chunk);
  }
}
```

### 3.1 快速对话（非流式，兼容旧版本）

**接口地址**: `POST /parallel-life/quick-chat`

**接口描述**: 快速对话接口，无需提供chatId，系统自动创建会话（推荐使用流式接口）

**请求参数**: 同流式接口

**响应示例**:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "content": "欢迎来到平行宇宙人生模拟器！...",
    "chatId": "550e8400-e29b-41d4-a716-446655440000"
  },
  "timestamp": 1704067200000
}
```

### 4. 生成平行人生报告

**接口地址**: `POST /parallel-life/report`

**接口描述**: 生成结构化的平行人生报告，包含多个平行宇宙的详细分析

**请求参数**:

```json
{
  "message": "我今年25岁，程序员，工作2年，在考虑是否应该继续做技术还是转行做产品经理",
  "chatId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "报告生成成功",
  "data": {
    "title": "张三的平行人生报告",
    "currentSituation": "25岁，程序员，工作2年，考虑转行",
    "universes": [
      {
        "name": "技术专家路径",
        "description": "继续深耕技术，成为领域专家",
        "timeline": "1年后：高级工程师，薪资15k→25k\n3年后：技术专家，薪资25k→40k...",
        "keyEvents": [
          "第2年：参与核心项目，获得技术突破",
          "第4年：发表技术文章，建立个人品牌"
        ],
        "metrics": "幸福感：75/100，财务状况：85/100，成长潜力：90/100",
        "probability": "70%"
      },
      {
        "name": "产品经理路径",
        "description": "转行做产品经理，负责产品规划",
        "timeline": "1年后：初级产品经理，薪资20k→25k\n3年后：高级产品经理，薪资25k→40k...",
        "keyEvents": [
          "第1年：完成产品经理培训",
          "第3年：主导重要产品上线"
        ],
        "metrics": "幸福感：80/100，财务状况：75/100，成长潜力：85/100",
        "probability": "65%"
      }
    ],
    "comparison": "技术路径稳定性高，风险低；产品路径成长空间大，但需要学习新技能...",
    "recommendations": [
      "建议先通过副业或内部转岗尝试产品工作",
      "保持技术能力，作为退路",
      "用6-12个月时间准备和过渡"
    ]
  },
  "timestamp": 1704067200000
}
```

### 5. 获取会话信息

**接口地址**: `GET /parallel-life/session/{chatId}`

**接口描述**: 根据会话ID获取会话信息

**路径参数**:
- `chatId`: 会话ID

**响应示例**:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "chatId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "user123",
    "sessionName": "我的职业规划",
    "createTime": "2025-01-31T10:00:00",
    "updateTime": "2025-01-31T10:00:00"
  },
  "timestamp": 1704067200000
}
```

## 使用场景示例

### 场景1：探索职业发展路径（流式）

```bash
# 1. 创建会话
POST /parallel-life/session
{
  "userId": "user123",
  "sessionName": "职业规划探索"
}

# 2. 开始流式对话（推荐）
POST /parallel-life/chat/stream
{
  "message": "我今年25岁，是一名程序员，工作2年了，想了解未来的职业发展路径",
  "chatId": "xxx",
  "useRag": true
}

# 3. 深入询问（流式）
POST /parallel-life/chat/stream
{
  "message": "如果我想转行做产品经理，需要做哪些准备？",
  "chatId": "xxx",
  "useRag": true
}

# 4. 生成报告
POST /parallel-life/report
{
  "message": "帮我生成一份详细的职业规划报告",
  "chatId": "xxx"
}
```

### 场景2：快速咨询

```bash
# 直接使用快速对话接口
POST /parallel-life/quick-chat
"我应该继续做技术还是转行做产品经理？"
```

### 场景3：使用工具增强

```bash
# 使用工具调用搜索行业数据
POST /parallel-life/chat
{
  "message": "帮我搜索一下2024年程序员转行做产品经理的成功率和薪资水平",
  "chatId": "xxx",
  "useTools": true
}

# 生成PDF报告
POST /parallel-life/chat
{
  "message": "生成一份'平行人生报告'PDF，包含多个宇宙的详细分析和建议",
  "chatId": "xxx",
  "useTools": true
}
```

## 错误处理

### 参数校验错误

```json
{
  "code": 400,
  "message": "消息内容不能为空",
  "data": null,
  "timestamp": 1704067200000
}
```

### 服务器错误

```json
{
  "code": 500,
  "message": "系统错误: xxx",
  "data": null,
  "timestamp": 1704067200000
}
```

## 注意事项

1. **流式接口（推荐）**: 
   - 推荐使用流式接口 `/chat/stream` 和 `/quick-chat/stream`
   - 流式接口使用 Server-Sent Events (SSE) 协议
   - 可以实时显示AI回复，提升用户体验
   - 前端需要处理流式数据，逐步显示内容

2. **会话管理**: 
   - 每个会话ID对应一个独立的对话上下文
   - 会话信息会持久化保存，服务重启后仍可恢复
   - 建议前端保存chatId，用于后续对话

3. **RAG增强**:
   - 使用RAG增强时，AI会结合知识库提供更专业的建议
   - 适合需要专业建议的场景

4. **工具调用**:
   - 使用工具调用时，AI可以搜索网络、生成PDF等
   - 适合需要实时数据或生成文档的场景
   - 注意：工具调用可能不支持流式输出

5. **报告生成**:
   - 报告生成需要较长时间，建议前端显示加载状态
   - 报告包含结构化的数据，适合前端展示
   - 报告生成不支持流式输出

6. **性能优化**:
   - 流式接口可以显著提升用户体验，减少等待时间
   - 非流式接口需要等待完整回复，响应时间取决于AI模型处理时间
   - 建议前端添加超时处理和重试机制

7. **异常处理**:
   - 所有异常由全局异常处理器统一处理
   - 流式接口中的异常会通过Flux.error返回
   - 前端需要处理流式数据中的错误情况

## Swagger文档

访问 `http://localhost:8123/api/swagger-ui.html` 查看完整的API文档，包括：
- 所有接口的详细说明
- 请求参数和响应格式
- 在线测试功能

