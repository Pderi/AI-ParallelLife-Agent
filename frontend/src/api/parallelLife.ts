import apiClient from './client'
import type {
  ApiResponse,
  CreateSessionRequest,
  SessionResponse,
  ChatRequest,
  ChatResponse,
  ReportRequest,
  ParallelLifeReport,
} from './types'

/**
 * 解析SSE数据块，处理各种格式：
 * - 标准格式：data: xxx
 * - 无空格格式：data:xxx
 * - 合并格式：data:欢迎data:来到
 * - 清理所有残留的 data: 前缀
 */
function parseSSEChunk(chunk: string, onChunk: (content: string) => void): void {
  let trimmed = chunk.trim()
  if (!trimmed) return

  // 首先处理SSE格式的 data: 前缀
  if (trimmed.startsWith('data: ')) {
    trimmed = trimmed.slice(6).trim()
  } else if (trimmed.startsWith('data:')) {
    trimmed = trimmed.slice(5).trim()
  } else if (trimmed.startsWith(':')) {
    // SSE注释行，跳过
    return
  }

  if (!trimmed) return

  // 处理可能的数据合并情况：data:欢迎data:来到
  // 使用正则表达式匹配所有 data: 后的内容
  const dataPattern = /data:\s*/g
  if (dataPattern.test(trimmed)) {
    // 包含多个 data: 标记，需要分割
    const parts = trimmed.split(/(?=data:)/)
    for (const part of parts) {
      let partContent = part.trim()
      if (!partContent) continue
      
      // 移除 data: 前缀
      if (partContent.startsWith('data: ')) {
        partContent = partContent.slice(6).trim()
      } else if (partContent.startsWith('data:')) {
        partContent = partContent.slice(5).trim()
      }
      
      // 再次清理可能残留的 data: 前缀（防止嵌套情况）
      partContent = partContent.replace(/^data:\s*/g, '')
      
      if (partContent) {
        onChunk(partContent)
      }
    }
  } else {
    // 单个数据块，再次清理可能残留的 data: 前缀
    trimmed = trimmed.replace(/^data:\s*/g, '')
    if (trimmed) {
      onChunk(trimmed)
    }
  }
}

/**
 * 创建会话
 */
export async function createSession(
  request?: CreateSessionRequest
): Promise<SessionResponse> {
  const response = await apiClient.post<ApiResponse<SessionResponse>>(
    '/parallel-life/session',
    request || {}
  )
  return response.data.data
}

/**
 * 基础对话（非流式）
 */
export async function chat(request: ChatRequest): Promise<ChatResponse> {
  const response = await apiClient.post<ApiResponse<ChatResponse>>(
    '/parallel-life/chat',
    request
  )
  return response.data.data
}

/**
 * 快速对话（非流式，自动创建会话）
 */
export async function quickChat(message: string): Promise<ChatResponse> {
  const response = await apiClient.post<ApiResponse<ChatResponse>>(
    '/parallel-life/quick-chat',
    message,
    {
      headers: {
        'Content-Type': 'application/json',
      },
    }
  )
  return response.data.data
}

/**
 * 生成报告
 */
export async function generateReport(
  request: ReportRequest
): Promise<ParallelLifeReport> {
  const response = await apiClient.post<ApiResponse<ParallelLifeReport>>(
    '/parallel-life/report',
    request
  )
  return response.data.data
}

/**
 * 获取会话信息
 */
export async function getSession(chatId: string): Promise<SessionResponse> {
  const response = await apiClient.get<ApiResponse<SessionResponse>>(
    `/parallel-life/session/${chatId}`
  )
  return response.data.data
}

/**
 * 流式对话（使用Fetch API处理SSE）
 */
export async function streamChat(
  request: ChatRequest,
  onChunk: (chunk: string) => void,
  onError?: (error: Error) => void,
  onComplete?: () => void
): Promise<() => void> {
  // 使用相对路径，让Vite代理处理（开发环境）或使用环境变量（生产环境）
  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

  const controller = new AbortController()

  try {
    const response = await fetch(`${API_BASE_URL}/parallel-life/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
      signal: controller.signal,
    })

    if (!response.ok) {
      let errorMessage = `HTTP error! status: ${response.status}`
      try {
        const errorData = await response.json()
        errorMessage = errorData.message || errorData.error || errorMessage
      } catch {
        // 如果不是JSON格式，尝试读取文本
        try {
          const errorText = await response.text()
          if (errorText) errorMessage = errorText
        } catch {
          // 忽略
        }
      }
      throw new Error(errorMessage)
    }

    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('无法获取响应流')
    }

    const decoder = new TextDecoder()
    let buffer = ''

    const readStream = async () => {
      try {
        while (true) {
          const { done, value } = await reader.read()
          if (done) {
            if (buffer.trim()) {
              // 处理剩余的buffer
              const trimmed = buffer.trim()
              if (trimmed && !trimmed.startsWith(':')) {
                // 使用统一的解析函数处理
                parseSSEChunk(trimmed, onChunk)
              }
            }
            onComplete?.()
            break
          }

          buffer += decoder.decode(value, { stream: true })
          
          // 处理SSE格式：可能是 "data: xxx\n\n" 或直接是文本流
          // 需要处理可能的数据合并情况，如 "data:欢迎data:来到"
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''

          for (const line of lines) {
            const trimmedLine = line.trim()
            if (!trimmedLine) continue // 跳过空行
            
            if (trimmedLine.startsWith(':')) {
              // SSE注释行，跳过
              continue
            }
            
            // 使用统一的解析函数处理
            parseSSEChunk(trimmedLine, onChunk)
          }
        }
      } catch (error) {
        if (error instanceof Error && error.name !== 'AbortError') {
          console.error('流式读取错误:', error)
          onError?.(error)
        }
      }
    }

    readStream().catch((error) => {
      console.error('流式读取异常:', error)
      if (error instanceof Error && error.name !== 'AbortError') {
        onError?.(error)
      }
    })

    // 返回取消函数
    return () => {
      controller.abort()
      reader.cancel()
    }
  } catch (error) {
    if (error instanceof Error) {
      onError?.(error)
    }
    return () => {}
  }
}

/**
 * 快速流式对话（自动创建会话）
 */
export async function quickStreamChat(
  message: string,
  onChunk: (chunk: string) => void,
  onError?: (error: Error) => void,
  onComplete?: () => void,
  chatId?: string
): Promise<() => void> {
  // 使用相对路径，让Vite代理处理（开发环境）或使用环境变量（生产环境）
  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

  const controller = new AbortController()

  try {
    // 如果有chatId，使用标准接口；否则使用快速接口
    const url = chatId 
      ? `${API_BASE_URL}/parallel-life/chat/stream`
      : `${API_BASE_URL}/parallel-life/quick-chat/stream`
    
    const body = chatId
      ? JSON.stringify({ message, chatId })
      : JSON.stringify(message)

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body,
      signal: controller.signal,
    })

    if (!response.ok) {
      let errorMessage = `HTTP error! status: ${response.status}`
      try {
        const errorData = await response.json()
        errorMessage = errorData.message || errorData.error || errorMessage
      } catch {
        // 如果不是JSON格式，尝试读取文本
        try {
          const errorText = await response.text()
          if (errorText) errorMessage = errorText
        } catch {
          // 忽略
        }
      }
      throw new Error(errorMessage)
    }

    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('无法获取响应流')
    }

    const decoder = new TextDecoder()
    let buffer = ''

    const readStream = async () => {
      try {
        while (true) {
          const { done, value } = await reader.read()
          if (done) {
            if (buffer.trim()) {
              // 处理剩余的buffer
              const trimmed = buffer.trim()
              if (trimmed && !trimmed.startsWith(':')) {
                // 使用统一的解析函数处理
                parseSSEChunk(trimmed, onChunk)
              }
            }
            onComplete?.()
            break
          }

          buffer += decoder.decode(value, { stream: true })
          
          // 处理SSE格式：可能是 "data: xxx\n\n" 或直接是文本流
          // 需要处理可能的数据合并情况，如 "data:欢迎data:来到"
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''

          for (const line of lines) {
            const trimmedLine = line.trim()
            if (!trimmedLine) continue // 跳过空行
            
            if (trimmedLine.startsWith(':')) {
              // SSE注释行，跳过
              continue
            }
            
            // 使用统一的解析函数处理
            parseSSEChunk(trimmedLine, onChunk)
          }
        }
      } catch (error) {
        if (error instanceof Error && error.name !== 'AbortError') {
          console.error('流式读取错误:', error)
          onError?.(error)
        }
      }
    }

    readStream().catch((error) => {
      console.error('流式读取异常:', error)
      if (error instanceof Error && error.name !== 'AbortError') {
        onError?.(error)
      }
    })

    return () => {
      controller.abort()
      reader.cancel()
    }
  } catch (error) {
    if (error instanceof Error) {
      onError?.(error)
    }
    return () => {}
  }
}

