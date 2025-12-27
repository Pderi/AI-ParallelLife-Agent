import React from 'react'
import { Spin } from 'antd'

interface LoadingProps {
  tip?: string
}

export const Loading: React.FC<LoadingProps> = ({ tip = '加载中...' }) => {
  return (
    <div style={{ textAlign: 'center', padding: '50px' }}>
      <Spin size="large" tip={tip} />
    </div>
  )
}

