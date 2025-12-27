# 环境变量配置说明

## 必需的环境变量

创建 `.env` 文件（不会被提交到Git），包含以下变量：

```env
# API基础地址
VITE_API_BASE_URL=http://localhost:8123/api

# 应用标题
VITE_APP_TITLE=平行宇宙人生模拟器
```

## 环境变量说明

### VITE_API_BASE_URL

后端API的基础地址。默认值为 `http://localhost:8123/api`。

如果后端运行在不同地址或端口，请修改此值。

示例：
- 本地开发: `http://localhost:8123/api`
- 生产环境: `https://api.example.com/api`

### VITE_APP_TITLE

应用标题，显示在浏览器标签页和应用头部。

## 注意事项

1. `.env` 文件不会被提交到Git（已在 `.gitignore` 中）
2. 修改环境变量后需要重启开发服务器
3. 环境变量必须以 `VITE_` 开头才能在代码中访问
4. 生产环境构建时，环境变量会被内联到代码中

