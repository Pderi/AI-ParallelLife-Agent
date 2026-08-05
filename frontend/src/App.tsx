import React, { useEffect, useState } from 'react'
import { Layout, ConfigProvider, theme, Button, message } from 'antd'
import { FileTextOutlined } from '@ant-design/icons'
import { ChatList, ChatInput } from '@/components/Chat'
import { ErrorBoundary } from '@/components/common/ErrorBoundary'
import { ReportModal } from '@/components/Report/ReportModal'
import { useChat } from '@/hooks/useChat'
import { createSession, generateReport } from '@/api/parallelLife'
import { useChatStore } from '@/store/chatStore'
import { APP_TITLE } from '@/utils/constants'
import type { ParallelLifeReport } from '@/api/types'
import './App.css'

const { Header, Content, Footer } = Layout

function App() {
  const { messages, isStreaming, quickSendMessage, stopStreaming, currentChatId } = useChat()
  const { setCurrentChatId } = useChatStore()
  const [isDark, setIsDark] = useState(false)
  const [report, setReport] = useState<ParallelLifeReport | null>(null)
  const [reportVisible, setReportVisible] = useState(false)
  const [generatingReport, setGeneratingReport] = useState(false)
  const [initError, setInitError] = useState<string | null>(null)

  // 初始化会话
  useEffect(() => {
    const initSession = async () => {
      try {
        const session = await createSession({
          sessionName: '新对话',
        })
        setCurrentChatId(session.chatId)
        setInitError(null)
      } catch (error) {
        console.error('创建会话失败:', error)
        setInitError(error instanceof Error ? error.message : '创建会话失败')
        // 即使创建会话失败，也生成一个临时ID，避免页面无法使用
        const tempChatId = `temp-${Date.now()}-${Math.random().toString(36).substring(2, 11)}`
        setCurrentChatId(tempChatId)
      }
    }
    initSession()
  }, [setCurrentChatId])

  const handleSend = async (
    message: string,
    options?: { useRag?: boolean; useTools?: boolean }
  ) => {
    try {
      await quickSendMessage(message, options)
    } catch (error) {
      console.error('发送消息失败:', error)
    }
  }

  const handleGenerateReport = async () => {
    if (!currentChatId) {
      message.warning('请先开始对话')
      return
    }

    if (messages.length === 0) {
      message.warning('请先发送消息')
      return
    }

    setGeneratingReport(true)
    try {
      // 使用最后一条用户消息生成报告
      const lastUserMessage = [...messages].reverse().find(msg => msg.role === 'user')
      if (!lastUserMessage) {
        message.warning('未找到用户消息')
        return
      }

      const reportData = await generateReport({
        message: lastUserMessage.content,
        chatId: currentChatId,
      })

      setReport(reportData)
      setReportVisible(true)
      message.success('报告生成成功')
    } catch (error) {
      console.error('生成报告失败:', error)
      message.error(error instanceof Error ? error.message : '生成报告失败')
    } finally {
      setGeneratingReport(false)
    }
  }

  // 如果初始化失败，显示错误提示但继续渲染页面
  if (initError) {
    console.warn('初始化警告:', initError)
  }

  return (
    <ConfigProvider
      theme={{
        algorithm: isDark ? theme.darkAlgorithm : theme.defaultAlgorithm,
        token: {
          colorPrimary: isDark ? '#d4936a' : '#b8734a',
          colorBgContainer: isDark ? '#2a2927' : '#ffffff',
          borderRadius: 8,
          fontFamily:
            "-apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif",
        },
      }}
    >
      <ErrorBoundary>
        <Layout className="app-layout" data-theme={isDark ? 'dark' : 'light'}>
          <Header className="app-header">
            <div className="header-content">
              <h1 className="app-title">{APP_TITLE}</h1>
              <div className="header-actions">
                <Button
                  type="primary"
                  icon={<FileTextOutlined />}
                  onClick={handleGenerateReport}
                  loading={generatingReport}
                  disabled={!currentChatId || messages.length === 0}
                  style={{
                    marginRight: 12,
                    borderRadius: 8,
                  }}
                >
                  生成报告
                </Button>
                <button
                  className="theme-toggle"
                  onClick={() => setIsDark(!isDark)}
                  title={isDark ? '切换到浅色模式' : '切换到深色模式'}
                >
                  {isDark ? '☀️' : '🌙'}
                </button>
              </div>
            </div>
          </Header>
          <Content className="app-content">
            <ChatList messages={messages} />
            <div className="chat-input-container-wrapper">
              <ChatInput
                onSend={handleSend}
                onStop={stopStreaming}
                disabled={false}
                isStreaming={isStreaming}
              />
            </div>
          </Content>
          <Footer className="app-footer">
            <div className="footer-content">
              <span>© 2025 平行宇宙人生模拟器</span>
            </div>
          </Footer>
        </Layout>
        <ReportModal
          report={report}
          visible={reportVisible}
          onClose={() => setReportVisible(false)}
        />
      </ErrorBoundary>
    </ConfigProvider>
  )
}

export default App

