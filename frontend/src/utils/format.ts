import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

/**
 * 格式化时间
 */
export function formatTime(timestamp: number): string {
  return dayjs(timestamp).format('YYYY-MM-DD HH:mm:ss')
}

/**
 * 格式化相对时间
 */
export function formatRelativeTime(timestamp: number): string {
  return dayjs(timestamp).fromNow()
}

/**
 * 生成唯一ID
 */
export function generateId(): string {
  return `${Date.now()}-${Math.random().toString(36).substring(2, 11)}`
}

