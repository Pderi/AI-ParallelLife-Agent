import React from 'react'
import { Card, Tag, Space, Button } from 'antd'
import { BulbOutlined, StarOutlined, ShareAltOutlined } from '@ant-design/icons'
import { motion } from 'framer-motion'
import './RecommendationsList.css'

interface RecommendationsListProps {
  recommendations: string[]
}

export const RecommendationsList: React.FC<RecommendationsListProps> = ({ recommendations }) => {
  if (recommendations.length === 0) {
    return (
      <div className="recommendations-empty">
        <p>暂无建议</p>
      </div>
    )
  }

  // 简单的优先级判断（可以根据内容分析）
  const getPriority = (index: number): number => {
    // 前3个建议优先级较高
    return index < 3 ? 5 - index : 3
  }

  const getDifficulty = (text: string): 'easy' | 'medium' | 'hard' => {
    if (text.includes('建议') || text.includes('可以')) return 'easy'
    if (text.includes('需要') || text.includes('准备')) return 'medium'
    return 'hard'
  }

  return (
    <div className="recommendations-list">
      <div className="recommendations-header">
        <h3>
          <BulbOutlined /> 专业建议
        </h3>
      </div>
      <div className="recommendations-grid">
        {recommendations.map((recommendation, index) => {
          const priority = getPriority(index)
          const difficulty = getDifficulty(recommendation)

          return (
            <motion.div
              key={index}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.4, delay: index * 0.1 }}
            >
              <Card
                className="recommendation-card"
                hoverable
                style={{
                  borderLeft: `4px solid ${
                    priority >= 4 ? '#52c41a' : priority >= 3 ? '#1890ff' : '#faad14'
                  }`,
                }}
              >
                <div className="recommendation-header">
                  <div className="recommendation-number">#{index + 1}</div>
                  <Space>
                    <Tag
                      color={priority >= 4 ? 'success' : priority >= 3 ? 'processing' : 'warning'}
                      style={{ borderRadius: 12 }}
                    >
                      {Array(priority)
                        .fill(0)
                        .map((_, i) => (
                          <StarOutlined key={i} />
                        ))}
                    </Tag>
                    <Tag
                      color={
                        difficulty === 'easy'
                          ? 'green'
                          : difficulty === 'medium'
                          ? 'blue'
                          : 'orange'
                      }
                      style={{ borderRadius: 12 }}
                    >
                      {difficulty === 'easy' ? '简单' : difficulty === 'medium' ? '中等' : '困难'}
                    </Tag>
                  </Space>
                </div>
                <div className="recommendation-content">
                  <p>{recommendation}</p>
                </div>
                <div className="recommendation-actions">
                  <Button
                    type="text"
                    icon={<ShareAltOutlined />}
                    size="small"
                    style={{ borderRadius: 8 }}
                  >
                    分享
                  </Button>
                </div>
              </Card>
            </motion.div>
          )
        })}
      </div>
    </div>
  )
}

