import { create } from 'zustand'
import type { Message } from '@/api/types'
import { generateId } from '@/utils/format'

interface ChatState {
  messages: Message[]
  currentChatId: string | null
  isStreaming: boolean
  addMessage: (message: Omit<Message, 'id' | 'timestamp'>) => string // 返回生成的ID
  updateMessage: (id: string, updates: Partial<Message>) => void
  appendToMessage: (id: string, content: string) => void
  setCurrentChatId: (chatId: string | null) => void
  clearMessages: () => void
  setIsStreaming: (isStreaming: boolean) => void
}

export const useChatStore = create<ChatState>((set) => ({
  messages: [],
  currentChatId: null,
  isStreaming: false,

  addMessage: (message) => {
    const id = generateId()
    set((state) => ({
      messages: [
        ...state.messages,
        {
          ...message,
          id,
          timestamp: Date.now(),
        },
      ],
    }))
    return id
  },

  updateMessage: (id, updates) =>
    set((state) => ({
      messages: state.messages.map((msg) =>
        msg.id === id ? { ...msg, ...updates } : msg
      ),
    })),

  appendToMessage: (id, content) =>
    set((state) => {
      const updatedMessages = state.messages.map((msg) => {
        if (msg.id === id) {
          // 清理可能残留的 data: 前缀
          let cleanContent = content.replace(/^data:\s*/g, '').replace(/\bdata:\s*/g, '')
          const newContent = msg.content + cleanContent
          console.log('追加消息内容, ID:', id, '原内容长度:', msg.content.length, '新内容长度:', newContent.length, '追加内容:', cleanContent)
          return { ...msg, content: newContent }
        }
        return msg
      })
      // 检查是否找到对应的消息
      const found = updatedMessages.some(msg => msg.id === id)
      if (!found) {
        console.error('未找到消息ID:', id, '当前消息列表:', state.messages.map(m => ({ id: m.id, role: m.role })))
      }
      return { messages: updatedMessages }
    }),

  setCurrentChatId: (chatId) => set({ currentChatId: chatId }),

  clearMessages: () => set({ messages: [] }),

  setIsStreaming: (isStreaming) => set({ isStreaming }),
}))
