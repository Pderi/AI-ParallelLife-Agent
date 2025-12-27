import axios, { AxiosInstance, AxiosError } from 'axios'
import { ApiResponse } from './types'

// 使用相对路径，让Vite代理处理（开发环境）或使用环境变量（生产环境）
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

/**
 * 创建axios实例
 */
export const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 300000, // 5分钟超时（流式接口需要较长超时）
  headers: {
    'Content-Type': 'application/json',
  },
})

/**
 * 请求拦截器
 */
apiClient.interceptors.request.use(
  (config) => {
    // 可以在这里添加认证token等
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

/**
 * 响应拦截器
 */
apiClient.interceptors.response.use(
  (response) => {
    return response
  },
  (error: AxiosError) => {
    // 统一错误处理
    if (error.response) {
      // 服务器返回了错误状态码
      const data = error.response.data as ApiResponse
      const errorMessage = data?.message || `请求失败: ${error.response.status}`
      return Promise.reject(new Error(errorMessage))
    } else if (error.request) {
      // 请求已发出但没有收到响应
      return Promise.reject(new Error('网络错误，请检查网络连接'))
    } else {
      // 其他错误
      return Promise.reject(new Error(error.message || '请求失败'))
    }
  }
)

export default apiClient

