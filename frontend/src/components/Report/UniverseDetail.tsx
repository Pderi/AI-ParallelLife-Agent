import React from 'react'
import { Drawer, Typography, Divider, Tag, Space } from 'antd'
import { CloseOutlined } from '@ant-design/icons'
import { motion } from 'framer-motion'
import type { Universe } from '@/api/types'
import { parseProbability, calculateOverallScore, getUniverseColor } from '@/utils/reportParser'
import { Timeline } from './Timeline'
import { MetricsChart } from './MetricsChart'
import './UniverseDetail.css'

const { Title, Paragraph } = Typography

interface UniverseDetailProps {
  universe: Universe | null
  index: number
  visible: boolean
  onClose: () => void
}

export const UniverseDetail: React.FC<UniverseDetailProps> = ({
  universe,
  index,
  visible,
  onClose,
}) => {
  if (!universe) return null

  const probability = parseProbability(universe.probability)
  const overallScore = calculateOverallScore(universe)
  const colors = getUniverseColor(index)

  return (
    <Drawer
      title={
        <div className="universe-detail-header">
          <div className="universe-detail-icon" style={{ background: colors.gradient }}>
            🌟
          </div>
          <div>
            <Title level={4} style={{ margin: 0 }}>
              {universe.name}
            </Title>
            <Space style={{ marginTop: 8 }}>
              <Tag color={colors.primary} style={{ borderRadius: 12 }}>
                概率: {probability}%
              </Tag>
              <Tag color={colors.secondary} style={{ borderRadius: 12 }}>
                评分: {overallScore}/100
              </Tag>
            </Space>
          </div>
        </div>
      }
      placement="right"
      onClose={onClose}
      open={visible}
      width={600}
      closeIcon={<CloseOutlined />}
      className="universe-detail-drawer"
    >
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.3 }}
      >
        <div className="universe-detail-content">
          <section className="detail-section">
            <Title level={5}>宇宙描述</Title>
            <Paragraph>{universe.description}</Paragraph>
          </section>

          <Divider />

          <section className="detail-section">
            <Title level={5}>时间线</Title>
            <Timeline timelineString={universe.timeline} />
          </section>

          <Divider />

          <section className="detail-section">
            <Title level={5}>关键事件</Title>
            <div className="key-events">
              {universe.keyEvents.map((event, idx) => (
                <motion.div
                  key={idx}
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ duration: 0.3, delay: idx * 0.1 }}
                  className="key-event-item"
                >
                  <div className="event-icon">🎯</div>
                  <div className="event-content">{event}</div>
                </motion.div>
              ))}
            </div>
          </section>

          <Divider />

          <section className="detail-section">
            <Title level={5}>人生指标</Title>
            <MetricsChart universe={universe} index={index} />
          </section>

          <Divider />

          <section className="detail-section">
            <Title level={5}>实现概率</Title>
            <div className="probability-display">
              <div className="probability-bar">
                <motion.div
                  className="probability-fill"
                  initial={{ width: 0 }}
                  animate={{ width: `${probability}%` }}
                  transition={{ duration: 1, ease: 'easeOut' }}
                  style={{ background: colors.gradient }}
                />
              </div>
              <div className="probability-text">{probability}%</div>
            </div>
            <Paragraph type="secondary" style={{ marginTop: 12 }}>
              {universe.probability}
            </Paragraph>
          </section>
        </div>
      </motion.div>
    </Drawer>
  )
}

