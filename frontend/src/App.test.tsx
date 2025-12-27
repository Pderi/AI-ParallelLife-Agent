// 临时测试文件 - 用于排查页面空白问题
// 如果这个简单版本能显示，说明问题在复杂组件中

import React from 'react'

export function TestApp() {
  return (
    <div style={{ padding: '20px', background: '#fff', minHeight: '100vh' }}>
      <h1>测试页面</h1>
      <p>如果你能看到这个，说明React正常工作</p>
    </div>
  )
}

