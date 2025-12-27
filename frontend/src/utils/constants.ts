/**
 * 应用常量
 */
export const APP_TITLE = import.meta.env.VITE_APP_TITLE || '平行宇宙人生模拟器'
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8123/api'

/**
 * 本地存储键名
 */
export const STORAGE_KEYS = {
  CHAT_ID: 'parallel_life_chat_id',
  THEME: 'parallel_life_theme',
  SESSIONS: 'parallel_life_sessions',
} as const

/**
 * 消息状态
 */
export const MESSAGE_STATUS = {
  SENDING: 'sending',
  SUCCESS: 'success',
  ERROR: 'error',
  STREAMING: 'streaming',
} as const

