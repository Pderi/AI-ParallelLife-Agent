import React from 'react'
import { Card, Tag, Button, Space } from 'antd'
import { StarOutlined, EyeOutlined, SwapOutlined } from '@ant-design/icons'
import { motion } from 'framer-motion'
import type { Universe } from '@/api/types'
import { parseProbability, calculateOverallScore, getUniverseColor } from '@/utils/reportParser'
import './UniverseCard.css'

interface UniverseCardProps {
  universe: Universe
  index: number
  onViewDetail?: (universe: Universe) => void
  onCompare?: (universe: Universe) => void
}

export const UniverseCard: React.FC<UniverseCardProps> = ({
  universe,
  index,
  onViewDetail,
  onCompare,
}) => {
  const probability = parseProbability(universe.probability)
  const overallScore = calculateOverallScore(universe)
  const colors = getUniverseColor(index)

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay: index * 0.1 }}
      whileHover={{ y: -4, transition: { duration: 0.2 } }}
    >
      <Card
        className="universe-card"
        style={{
          background: 'var(--color-surface-elevated)',
          backdropFilter: 'blur(20px)',
          border: `2px solid ${colors.primary}40`,
          borderRadius: 20,
          boxShadow: `0 4px 20px ${colors.primary}20`,
        }}
        hoverable
      >
        <div className="universe-card-header">
          <div className="universe-icon" style={{ background: colors.gradient }}>
            🌟
          </div>
          <div className="universe-title-section">
            <h3 className="universe-name">{universe.name}</h3>
            <div className="universe-tags">
              <Tag color={colors.primary} style={{ borderRadius: 12 }}>
                概率: {probability}%
              </Tag>
              <Tag color={colors.secondary} style={{ borderRadius: 12 }}>
                评分: {overallScore}/100
              </Tag>
            </div>
          </div>
        </div>

        <div className="universe-description">
          <p>{universe.description}</p>
        </div>

        <div className="universe-stats">
          <div className="stat-item">
            <span className="stat-label">实现概率</span>
            <div className="stat-bar">
              <div
                className="stat-bar-fill"
                style={{
                  width: `${probability}%`,
                  background: colors.gradient,
                }}
              />
            </div>
            <span className="stat-value">{probability}%</span>
          </div>
          <div className="stat-item">
            <span className="stat-label">综合评分</span>
            <div className="stat-bar">
              <div
                className="stat-bar-fill"
                style={{
                  width: `${overallScore}%`,
                  background: colors.gradient,
                }}
              />
            </div>
            <span className="stat-value">{overallScore}/100</span>
          </div>
        </div>

        <div className="universe-actions">
          <Space>
            <Button
              type="primary"
              icon={<EyeOutlined />}
              onClick={() => onViewDetail?.(universe)}
              style={{
                background: colors.gradient,
                border: 'none',
                borderRadius: 12,
              }}
            >
              查看详情
            </Button>
            <Button
              icon={<SwapOutlined />}
              onClick={() => onCompare?.(universe)}
              style={{ borderRadius: 12 }}
            >
              对比
            </Button>
          </Space>
        </div>
      </Card>
    </motion.div>
  )
}

