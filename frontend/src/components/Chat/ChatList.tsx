import React, { useEffect, useRef } from 'react'
import { Empty } from 'antd'
import { ChatMessage } from './ChatMessage'
import type { Message } from '@/api/types'
import './ChatList.css'

interface ChatListProps {
  messages: Message[]
}

export const ChatList: React.FC<ChatListProps> = ({ messages }) => {
  const listRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    // 自动滚动到底部
    if (listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight
    }
  }, [messages])

  if (messages.length === 0) {
    return (
      <div className="chat-list-empty">
        <div className="empty-content">
          <div className="empty-icon">🌌</div>
          <h2 className="empty-title">平行宇宙人生模拟器</h2>
          <p className="empty-description">
            探索不同人生路径的可能性<br />
            让AI为你模拟多个平行宇宙的未来
          </p>
          <div className="empty-features">
            <div className="feature-item">✨ 智能对话</div>
            <div className="feature-item">📊 多维度分析</div>
            <div className="feature-item">🎯 专业建议</div>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="chat-list" ref={listRef}>
      <div className="chat-list-content">
        {messages.map((message) => (
          <ChatMessage key={message.id} message={message} />
        ))}
      </div>
    </div>
  )
}

