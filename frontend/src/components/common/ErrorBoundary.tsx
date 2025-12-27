import React, { Component, ErrorInfo, ReactNode } from 'react'
import { Result, Button } from 'antd'

interface Props {
  children: ReactNode
}

interface State {
  hasError: boolean
  error?: Error
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
  }

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo)
  }

  public render() {
    if (this.state.hasError) {
      return (
        <Result
          status="error"
          title="出现了一些问题"
          subTitle={this.state.error?.message || '未知错误'}
          extra={
            <Button type="primary" onClick={() => window.location.reload()}>
              刷新页面
            </Button>
          }
        />
      )
    }

    return this.props.children
  }
}

