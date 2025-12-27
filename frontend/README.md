# 平行宇宙人生模拟器 - 前端应用

基于 React + TypeScript + Vite 构建的现代化前端应用，用于与平行宇宙人生模拟器后端进行交互。

## ✨ 功能特性

- 🤖 **智能对话**：与AI进行多轮对话，探索人生可能性
- 🔄 **流式响应**：支持Server-Sent Events (SSE)实时流式输出
- 📚 **RAG增强**：可选的知识库增强功能
- 🛠️ **工具调用**：可选的工具调用功能（搜索、PDF生成等）
- 💾 **会话管理**：自动创建和管理对话会话
- 🎨 **现代UI**：基于Ant Design的现代化界面
- 🌓 **主题切换**：支持深色/浅色主题切换
- 📱 **响应式设计**：支持桌面端和移动端

## 🛠️ 技术栈

- **框架**: React 18 + TypeScript
- **构建工具**: Vite
- **UI框架**: Ant Design 5
- **状态管理**: Zustand
- **HTTP客户端**: Axios
- **Markdown渲染**: react-markdown
- **路由**: React Router (预留)

## 📦 安装依赖

```bash
cd frontend
npm install
# 或
pnpm install
```

## 🚀 开发

### 环境变量配置

创建 `.env` 文件（参考 `.env.example`）：

```env
VITE_API_BASE_URL=http://localhost:8123/api
VITE_APP_TITLE=平行宇宙人生模拟器
```

### 启动开发服务器

```bash
npm run dev
# 或
pnpm dev
```

应用将在 `http://localhost:3000` 启动。

### 确保后端服务运行

前端需要后端API服务运行在 `http://localhost:8123/api`。如果后端运行在不同地址，请修改 `.env` 文件中的 `VITE_API_BASE_URL`。

## 📦 构建

### 生产构建

```bash
npm run build
# 或
pnpm build
```

构建产物将输出到 `dist` 目录。

### 预览构建结果

```bash
npm run preview
# 或
pnpm preview
```

## 🧪 代码规范

### Lint检查

```bash
npm run lint
```

### 代码格式化

```bash
npm run format
```

## 📁 项目结构

```
frontend/
├── public/                 # 静态资源
├── src/
│   ├── api/               # API调用
│   │   ├── client.ts      # HTTP客户端配置
│   │   ├── parallelLife.ts # 平行人生API
│   │   └── types.ts       # API类型定义
│   ├── components/        # 组件
│   │   ├── Chat/          # 对话相关组件
│   │   └── common/        # 通用组件
│   ├── hooks/             # 自定义Hooks
│   │   └── useChat.ts     # 对话逻辑
│   ├── store/             # 状态管理
│   │   └── chatStore.ts   # 对话状态
│   ├── utils/             # 工具函数
│   │   ├── constants.ts   # 常量定义
│   │   └── format.ts      # 格式化工具
│   ├── styles/            # 样式文件
│   │   └── index.css      # 全局样式
│   ├── App.tsx            # 根组件
│   ├── main.tsx           # 入口文件
│   └── vite-env.d.ts      # 类型声明
├── .eslintrc.cjs         # ESLint配置
├── .prettierrc           # Prettier配置
├── index.html            # HTML模板
├── package.json          # 依赖配置
├── tsconfig.json         # TypeScript配置
├── vite.config.ts        # Vite配置
└── README.md             # 项目说明
```

## 🔌 API对接

### 核心接口

- **创建会话**: `POST /api/parallel-life/session`
- **流式对话**: `POST /api/parallel-life/chat/stream` (SSE)
- **快速流式对话**: `POST /api/parallel-life/quick-chat/stream` (SSE)
- **生成报告**: `POST /api/parallel-life/report`

详细API文档请参考后端项目的 `docs/API接口文档.md`。

## 🎯 核心功能说明

### 流式对话

前端使用 Fetch API 处理 Server-Sent Events (SSE) 流式响应，实时显示AI回复内容。

### 会话管理

- 应用启动时自动创建会话
- 会话ID存储在状态管理中
- 支持多轮对话，保持上下文

### 消息管理

- 消息列表实时更新
- 支持Markdown渲染
- 支持消息复制
- 流式消息显示打字效果

### 错误处理

- 网络错误提示
- API错误信息显示
- 友好的错误提示界面

## 🐛 常见问题

### 1. 无法连接到后端API

- 检查后端服务是否运行在 `http://localhost:8123`
- 检查 `.env` 文件中的 `VITE_API_BASE_URL` 配置
- 检查浏览器控制台的网络请求错误

### 2. 流式响应不工作

- 检查后端是否支持SSE
- 检查浏览器是否支持EventSource API
- 查看浏览器控制台的错误信息

### 3. 样式显示异常

- 确保Ant Design样式正确加载
- 检查CSS文件是否正确导入

## 📝 开发注意事项

1. **流式处理**: SSE流式处理是核心功能，注意处理各种边界情况
2. **错误处理**: 网络不稳定时要有良好的错误提示和重试机制
3. **性能优化**: 长时间对话时注意消息列表的性能优化
4. **类型安全**: 充分利用TypeScript，避免使用`any`

## 🔗 相关文档

- [后端API文档](../docs-back/API接口文档.md)
- [业务流程文档](../docs-back/业务流程文档.md)
- [前端初始化提示词](../docs-front/前端初始化提示词.md)

## 📄 许可证

MIT

---

**版本**: 0.0.1  
**最后更新**: 2025-01-31

