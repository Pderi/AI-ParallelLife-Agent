import React, { useMemo } from 'react'
import { Radar, RadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis, ResponsiveContainer } from 'recharts'
import { motion } from 'framer-motion'
import type { Universe } from '@/api/types'
import { parseMetrics, getUniverseColor } from '@/utils/reportParser'
import './MetricsChart.css'

interface MetricsChartProps {
  universe: Universe
  index: number
  className?: string
}

export const MetricsChart: React.FC<MetricsChartProps> = ({ universe, index, className }) => {
  const colors = getUniverseColor(index)
  const metrics = parseMetrics(universe.metrics)

  const chartData = useMemo(() => {
    const data = [
      { subject: '幸福感', value: metrics.happiness || 0, fullMark: 100 },
      { subject: '财务状况', value: metrics.financial || 0, fullMark: 100 },
      { subject: '成长潜力', value: metrics.growth || 0, fullMark: 100 },
      { subject: '稳定性', value: metrics.stability || 0, fullMark: 100 },
      { subject: '工作生活平衡', value: metrics.workLifeBalance || 0, fullMark: 100 },
    ]
    return data.filter(item => item.value > 0) // 只显示有值的指标
  }, [metrics])

  if (chartData.length === 0) {
    return (
      <div className={`metrics-chart-empty ${className || ''}`}>
        <p>暂无指标数据</p>
      </div>
    )
  }

  return (
    <motion.div
      className={`metrics-chart ${className || ''}`}
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.5 }}
    >
      <ResponsiveContainer width="100%" height={300}>
        <RadarChart data={chartData}>
          <PolarGrid stroke="#e0e0e0" />
          <PolarAngleAxis
            dataKey="subject"
            tick={{ fill: '#666', fontSize: 12 }}
            tickLine={{ stroke: '#999' }}
          />
          <PolarRadiusAxis
            angle={90}
            domain={[0, 100]}
            tick={{ fill: '#999', fontSize: 10 }}
            tickCount={5}
          />
          <Radar
            name={universe.name}
            dataKey="value"
            stroke={colors.primary}
            fill={colors.primary}
            fillOpacity={0.6}
            strokeWidth={2}
            animationDuration={1500}
            animationEasing="ease-out"
          />
        </RadarChart>
      </ResponsiveContainer>
    </motion.div>
  )
}

