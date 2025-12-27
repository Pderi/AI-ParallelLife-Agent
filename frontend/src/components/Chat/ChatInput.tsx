import React, { useState, useRef, useEffect } from 'react'
import { Input, Button, Space, Checkbox, message } from 'antd'
import { SendOutlined, StopOutlined } from '@ant-design/icons'
import './ChatInput.css'

const { TextArea } = Input

interface ChatInputProps {
  onSend: (message: string, options?: { useRag?: boolean; useTools?: boolean }) => void
  onStop?: () => void
  disabled?: boolean
  isStreaming?: boolean
}

export const ChatInput: React.FC<ChatInputProps> = ({
  onSend,
  onStop,
  disabled = false,
  isStreaming = false,
}) => {
  const [inputValue, setInputValue] = useState('')
  const [useRag, setUseRag] = useState(false)
  const [useTools, setUseTools] = useState(false)
  const textAreaRef = useRef<any>(null)

  const handleSend = () => {
    if (!inputValue.trim()) {
      message.warning('请输入消息内容')
      return
    }

    if (disabled || isStreaming) {
      return
    }

    onSend(inputValue.trim(), { useRag, useTools })
    setInputValue('')
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  useEffect(() => {
    // 自动聚焦
    if (textAreaRef.current) {
      textAreaRef.current.focus()
    }
  }, [])

  return (
    <div className="chat-input-container">
      <div className="chat-input-options">
        <Space>
          <Checkbox
            checked={useRag}
            onChange={(e) => setUseRag(e.target.checked)}
            disabled={disabled || isStreaming}
          >
            RAG增强
          </Checkbox>
          <Checkbox
            checked={useTools}
            onChange={(e) => setUseTools(e.target.checked)}
            disabled={disabled || isStreaming}
          >
            工具调用
          </Checkbox>
        </Space>
      </div>
      <div className="chat-input-wrapper">
        <TextArea
          ref={textAreaRef}
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="输入你的问题... (Enter发送，Shift+Enter换行)"
          autoSize={{ minRows: 1, maxRows: 4 }}
          disabled={disabled || isStreaming}
          style={{ flex: 1 }}
        />
        <Button
          type="primary"
          icon={isStreaming ? <StopOutlined /> : <SendOutlined />}
          onClick={isStreaming ? onStop : handleSend}
          disabled={disabled || (!isStreaming && !inputValue.trim())}
          loading={isStreaming}
          style={{ marginLeft: 8, height: 'auto' }}
        >
          {isStreaming ? '停止' : '发送'}
        </Button>
      </div>
    </div>
  )
}

