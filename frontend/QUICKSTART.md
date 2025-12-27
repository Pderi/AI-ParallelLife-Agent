# 快速启动指南

## 📋 前置要求

1. **Node.js**: 版本 >= 18.0.0
2. **包管理器**: npm、pnpm 或 yarn
3. **后端服务**: 确保后端服务运行在 `http://localhost:8123`

## 🚀 快速开始

### 1. 安装依赖

```bash
cd frontend
npm install
# 或使用 pnpm（推荐）
pnpm install
```

### 2. 配置环境变量

创建 `.env` 文件：

```env
VITE_API_BASE_URL=http://localhost:8123/api
VITE_APP_TITLE=平行宇宙人生模拟器
```

### 3. 启动开发服务器

```bash
npm run dev
# 或
pnpm dev
```

应用将在 `http://localhost:3000` 启动。

### 4. 访问应用

打开浏览器访问：`http://localhost:3000`

## 🔧 开发命令

```bash
# 启动开发服务器
npm run dev

# 构建生产版本
npm run build

# 预览生产构建
npm run preview

# 代码检查
npm run lint

# 代码格式化
npm run format
```

## 📝 注意事项

1. **后端服务**: 确保后端服务已启动并运行在正确端口
2. **CORS**: 如果遇到CORS问题，检查后端CORS配置
3. **API地址**: 如果后端运行在不同地址，修改 `.env` 文件

## 🐛 常见问题

### 问题1: 无法连接到后端

**解决方案**:
- 检查后端服务是否运行
- 检查 `.env` 文件中的 `VITE_API_BASE_URL` 配置
- 查看浏览器控制台的网络请求

### 问题2: 依赖安装失败

**解决方案**:
- 清除缓存: `npm cache clean --force`
- 删除 `node_modules` 和 `package-lock.json`，重新安装
- 使用国内镜像: 已配置 `.npmrc` 使用淘宝镜像

### 问题3: 端口被占用

**解决方案**:
- 修改 `vite.config.ts` 中的 `server.port` 配置
- 或使用其他端口: `npm run dev -- --port 3001`

## 📚 下一步

- 查看 [README.md](./README.md) 了解详细文档
- 查看 [API接口文档](../docs-back/API接口文档.md) 了解后端API
- 查看 [业务流程文档](../docs-back/业务流程文档.md) 了解系统架构

