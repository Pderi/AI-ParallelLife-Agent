import React from 'react'
import { Avatar, Typography, Space, Button, message, Dropdown } from 'antd'
import { UserOutlined, RobotOutlined, CopyOutlined, MoreOutlined, ReloadOutlined } from '@ant-design/icons'
import type { MenuProps } from 'antd'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import type { Message as MessageType } from '@/api/types'
import { formatTime } from '@/utils/format'
import './ChatMessage.css'

const { Text } = Typography

interface ChatMessageProps {
  message: MessageType
}

export const ChatMessage: React.FC<ChatMessageProps> = ({ message }) => {
  const isUser = message.role === 'user'
  const isStreaming = message.status === 'streaming'

  const handleCopy = () => {
    navigator.clipboard.writeText(message.content)
    message.success('已复制到剪贴板')
  }

  const menuItems: MenuProps['items'] = [
    {
      key: 'copy',
      label: '复制',
      icon: <CopyOutlined />,
      onClick: handleCopy,
    },
    ...(isUser ? [] : [
      {
        key: 'reload',
        label: '重新生成',
        icon: <ReloadOutlined />,
        onClick: () => {
          message.info('重新生成功能开发中...')
        },
      },
    ]),
  ]

  return (
    <div className={`chat-message ${isUser ? 'user-message' : 'ai-message'}`}>
      <div className="chat-message-content">
        <Space align="start" size="middle">
          <Avatar
            size={48}
            icon={isUser ? <UserOutlined /> : <RobotOutlined />}
            style={{
              background: isUser
                ? 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
                : 'linear-gradient(135deg, #52c41a 0%, #73d13d 100%)',
              flexShrink: 0,
              boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
              border: '2px solid rgba(255, 255, 255, 0.3)',
            }}
          />
          <div className="message-body">
            <div className="message-header">
              <Text type="secondary" style={{ fontSize: 12 }}>
                {isUser ? '你' : 'AI助手'}
              </Text>
              <Text type="secondary" style={{ fontSize: 12, marginLeft: 8 }}>
                {formatTime(message.timestamp)}
              </Text>
            </div>
            <div className="message-text">
              {isUser ? (
                <Text>{message.content}</Text>
              ) : (
                <ReactMarkdown
                  remarkPlugins={[remarkGfm]}
                  rehypePlugins={[]}
                  components={{
                    p: ({ children }) => {
                      // 确保段落正确渲染，处理空段落
                      if (!children || (Array.isArray(children) && children.length === 0)) {
                        return <br />
                      }
                      return <p className="markdown-p">{children}</p>
                    },
                    h1: ({ children }) => <h1 className="markdown-h1">{children}</h1>,
                    h2: ({ children }) => <h2 className="markdown-h2">{children}</h2>,
                    h3: ({ children }) => <h3 className="markdown-h3">{children}</h3>,
                    h4: ({ children }) => <h4 className="markdown-h4">{children}</h4>,
                    ul: ({ children }) => <ul className="markdown-ul">{children}</ul>,
                    ol: ({ children }) => <ol className="markdown-ol">{children}</ol>,
                    li: ({ children }) => <li className="markdown-li">{children}</li>,
                    blockquote: ({ children }) => <blockquote className="markdown-blockquote">{children}</blockquote>,
                    code: ({ children, className }) => {
                      const isInline = !className
                      return isInline ? (
                        <code className="markdown-code-inline">{children}</code>
                      ) : (
                        <pre className="markdown-pre"><code className={className}>{children}</code></pre>
                      )
                    },
                    table: ({ children }) => (
                      <div className="markdown-table-wrapper">
                        <table className="markdown-table">{children}</table>
                      </div>
                    ),
                    thead: ({ children }) => <thead className="markdown-thead">{children}</thead>,
                    tbody: ({ children }) => <tbody className="markdown-tbody">{children}</tbody>,
                    tr: ({ children }) => <tr className="markdown-tr">{children}</tr>,
                    th: ({ children }) => <th className="markdown-th">{children}</th>,
                    td: ({ children }) => <td className="markdown-td">{children}</td>,
                    hr: () => <hr className="markdown-hr" />,
                    strong: ({ children }) => <strong className="markdown-strong">{children}</strong>,
                    em: ({ children }) => <em className="markdown-em">{children}</em>,
                    a: ({ children, href }) => (
                      <a href={href} target="_blank" rel="noopener noreferrer" className="markdown-link">
                        {children}
                      </a>
                    ),
                  }}
                >
                  {(() => {
                    let content = message.content
                      .replace(/^data:\s*/gm, '')
                      .replace(/\bdata:\s*/g, '')
                    
                    // 首先确保Markdown标题格式正确（## 标题）前后有空行
                    content = content.replace(/([^\n])(\n##+[^\n]+)/g, '$1\n\n$2')
                    content = content.replace(/(##+[^\n]+\n)([^\n])/g, '$1\n$2')
                    
                    // 在句号、问号、感叹号后添加换行（用于段落分隔）
                    content = content.replace(/([。！？])\s*([^\n。！？])/g, '$1\n\n$2')
                    
                    // 在冒号后添加换行（用于列表项或说明，但保留列表格式）
                    content = content.replace(/([:：])\s*([^\n:：])/g, (match, p1, p2) => {
                      // 如果后面是列表标记，不添加换行
                      if (/^[-•·\d\s]/.test(p2)) {
                        return match
                      }
                      // 如果后面是中文，添加换行
                      if (/[\u4e00-\u9fa5]/.test(p2)) {
                        return p1 + '\n' + p2
                      }
                      return match
                    })
                    
                    // 确保列表项格式正确
                    content = content.replace(/([^\n])(\n[-•·]\s)/g, '$1\n$2')
                    content = content.replace(/([-•·]\s[^\n]+\n)([^\n-•·])/g, '$1\n$2')
                    
                    // 在"时间线"、"指标"等关键词后添加换行
                    content = content.replace(/(时间线|指标|概率|风险|建议|描述|特点)[:：]\s*/g, '$1：\n')
                    
                    // 清理多余的空行（最多保留两个连续换行）
                    content = content.replace(/\n{4,}/g, '\n\n')
                    
                    return content
                  })()}
                </ReactMarkdown>
              )}
              {isStreaming && (
                <span className="streaming-indicator">▋</span>
              )}
            </div>
            {message.status === 'error' && (
              <Text type="danger" style={{ fontSize: 12 }}>
                消息发送失败
              </Text>
            )}
            {!isUser && (
              <div className="message-actions">
                {message.content && message.content.trim() && (
                  <Button
                    type="text"
                    size="small"
                    icon={<CopyOutlined />}
                    onClick={handleCopy}
                    className="action-button"
                  >
                    复制
                  </Button>
                )}
                <Dropdown 
                  menu={{ items: menuItems }} 
                  trigger={['click']} 
                  placement="bottomRight"
                  overlayClassName="message-dropdown"
                  getPopupContainer={(triggerNode) => triggerNode.parentElement || document.body}
                >
                  <Button
                    type="text"
                    size="small"
                    icon={<MoreOutlined />}
                    className="action-button more-button"
                    onClick={(e) => {
                      e.stopPropagation()
                      e.preventDefault()
                    }}
                  >
                    更多
                  </Button>
                </Dropdown>
              </div>
            )}
          </div>
        </Space>
      </div>
    </div>
  )
}

