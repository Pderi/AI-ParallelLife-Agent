import React from 'react'
import { Timeline as AntTimeline, Tag } from 'antd'
import { motion } from 'framer-motion'
import { parseTimeline } from '@/utils/reportParser'
import './Timeline.css'

interface TimelineProps {
  timelineString: string
  className?: string
}

export const Timeline: React.FC<TimelineProps> = ({ timelineString, className }) => {
  const timeline = parseTimeline(timelineString)

  if (timeline.length === 0) {
    return (
      <div className={`timeline-empty ${className || ''}`}>
        <p>暂无时间线数据</p>
      </div>
    )
  }

  return (
    <div className={`timeline-container ${className || ''}`}>
      <AntTimeline
        mode="left"
        items={timeline.map((item, index) => ({
          key: index,
          children: (
            <motion.div
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.4, delay: index * 0.1 }}
              className="timeline-item"
            >
              <div className="timeline-year">
                <Tag color="blue" style={{ borderRadius: 12, padding: '4px 12px' }}>
                  {item.year}
                </Tag>
              </div>
              <div className="timeline-content">
                <p>{item.description}</p>
              </div>
            </motion.div>
          ),
        }))}
      />
    </div>
  )
}

