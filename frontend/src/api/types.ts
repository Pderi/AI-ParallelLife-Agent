/**
 * API响应统一格式
 */
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

/**
 * 创建会话请求
 */
export interface CreateSessionRequest {
  userId?: string
  sessionName?: string
}

/**
 * 会话响应
 */
export interface SessionResponse {
  chatId: string
  userId: string | null
  sessionName: string | null
  createTime: string
  updateTime: string
}

/**
 * 对话请求
 */
export interface ChatRequest {
  message: string
  chatId: string
  useRag?: boolean
  useTools?: boolean
}

/**
 * 对话响应（非流式）
 */
export interface ChatResponse {
  content: string
  chatId: string
}

/**
 * 报告请求
 */
export interface ReportRequest {
  message: string
  chatId: string
}

/**
 * 平行宇宙
 */
export interface Universe {
  name: string
  description: string
  timeline: string
  keyEvents: string[]
  metrics: string
  probability: string
}

/**
 * 平行人生报告
 */
export interface ParallelLifeReport {
  title: string
  currentSituation: string
  universes: Universe[]
  comparison: string
  recommendations: string[]
}

/**
 * 消息类型
 */
export type MessageRole = 'user' | 'assistant'

/**
 * 消息状态
 */
export type MessageStatus = 'sending' | 'success' | 'error' | 'streaming'

/**
 * 消息数据
 */
export interface Message {
  id: string
  role: MessageRole
  content: string
  timestamp: number
  status: MessageStatus
  chatId?: string
}

