import { useCallback, useRef } from 'react'
import { useChatStore } from '@/store/chatStore'
import { streamChat, quickStreamChat } from '@/api/parallelLife'
import type { ChatRequest, Message } from '@/api/types'
import { generateId } from '@/utils/format'

export function useChat() {
  const {
    messages,
    currentChatId,
    isStreaming,
    addMessage,
    updateMessage,
    appendToMessage,
    setCurrentChatId,
    setIsStreaming,
  } = useChatStore()

  const abortControllerRef = useRef<(() => void) | null>(null)

  /**
   * 发送消息（流式）
   */
  const sendMessage = useCallback(
    async (
      content: string,
      options?: {
        useRag?: boolean
        useTools?: boolean
        chatId?: string
      }
    ) => {
      if (!content.trim() || isStreaming) {
        return
      }

      const chatId = options?.chatId || currentChatId
      if (!chatId) {
        throw new Error('会话ID不存在，请先创建会话')
      }

      // 添加用户消息
      const userMessage: Omit<Message, 'id' | 'timestamp'> = {
        role: 'user',
        content: content.trim(),
        status: 'success',
        chatId,
      }
      addMessage(userMessage)

      // 添加AI消息占位符，获取实际生成的ID
      const aiMessage: Omit<Message, 'id' | 'timestamp'> = {
        role: 'assistant',
        content: '',
        status: 'streaming',
        chatId,
      }
      const aiMessageId = addMessage(aiMessage)
      console.log('创建AI消息占位符, ID:', aiMessageId)

      setIsStreaming(true)

      try {
        const request: ChatRequest = {
          message: content.trim(),
          chatId,
          useRag: options?.useRag || false,
          useTools: options?.useTools || false,
        }

        abortControllerRef.current = await streamChat(
          request,
          (chunk) => {
            // 实时追加内容
            console.log('收到数据块:', chunk, '消息ID:', aiMessageId)
            appendToMessage(aiMessageId, chunk)
          },
          (error) => {
            // 错误处理
            updateMessage(aiMessageId, {
              status: 'error',
              content: `错误: ${error.message}`,
            })
            setIsStreaming(false)
          },
          () => {
            // 完成
            updateMessage(aiMessageId, {
              status: 'success',
            })
            setIsStreaming(false)
          }
        )
      } catch (error) {
        updateMessage(aiMessageId, {
          status: 'error',
          content: error instanceof Error ? error.message : '发送失败',
        })
        setIsStreaming(false)
      }
    },
    [currentChatId, isStreaming, addMessage, updateMessage, appendToMessage, setIsStreaming]
  )

  /**
   * 快速发送消息（自动创建会话）
   */
  const quickSendMessage = useCallback(
    async (
      content: string,
      options?: {
        useRag?: boolean
        useTools?: boolean
      }
    ) => {
      if (!content.trim() || isStreaming) {
        return
      }

      // 如果没有会话ID，先创建一个
      let chatId = currentChatId
      if (!chatId) {
        // 生成临时ID，实际应该调用创建会话API
        chatId = generateId()
        setCurrentChatId(chatId)
      }

      // 添加用户消息
      const userMessage: Omit<Message, 'id' | 'timestamp'> = {
        role: 'user',
        content: content.trim(),
        status: 'success',
        chatId,
      }
      addMessage(userMessage)

      // 添加AI消息占位符，获取实际生成的ID
      const aiMessage: Omit<Message, 'id' | 'timestamp'> = {
        role: 'assistant',
        content: '',
        status: 'streaming',
        chatId,
      }
      const aiMessageId = addMessage(aiMessage)
      console.log('创建AI消息占位符, ID:', aiMessageId)

      setIsStreaming(true)

      try {
        console.log('开始发送消息:', content.trim(), 'chatId:', chatId)
        abortControllerRef.current = await quickStreamChat(
          content.trim(),
          (chunk) => {
            console.log('收到数据块:', chunk, '消息ID:', aiMessageId)
            appendToMessage(aiMessageId, chunk)
            // 调试：检查消息是否更新
            const updatedMessages = useChatStore.getState().messages
            const updatedMsg = updatedMessages.find(m => m.id === aiMessageId)
            console.log('更新后的消息内容:', updatedMsg?.content)
          },
          (error) => {
            console.error('流式对话错误:', error)
            updateMessage(aiMessageId, {
              status: 'error',
              content: `错误: ${error.message}`,
            })
            setIsStreaming(false)
          },
          () => {
            console.log('流式对话完成')
            updateMessage(aiMessageId, {
              status: 'success',
            })
            setIsStreaming(false)
          },
          chatId // 传递chatId，确保使用同一个会话
        )
      } catch (error) {
        console.error('发送消息异常:', error)
        updateMessage(aiMessageId, {
          status: 'error',
          content: error instanceof Error ? error.message : '发送失败',
        })
        setIsStreaming(false)
      }
    },
    [currentChatId, isStreaming, addMessage, updateMessage, appendToMessage, setCurrentChatId, setIsStreaming]
  )

  /**
   * 停止流式响应
   */
  const stopStreaming = useCallback(() => {
    if (abortControllerRef.current) {
      abortControllerRef.current()
      abortControllerRef.current = null
      setIsStreaming(false)
    }
  }, [setIsStreaming])

  return {
    messages,
    currentChatId,
    isStreaming,
    sendMessage,
    quickSendMessage,
    stopStreaming,
  }
}

