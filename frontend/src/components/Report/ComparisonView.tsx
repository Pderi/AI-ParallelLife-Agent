import React, { useState } from 'react'
import { Card, Tabs, Table, Space, Tag, Radio } from 'antd'
import { TableOutlined, RadarChartOutlined, BarChartOutlined } from '@ant-design/icons'
import { motion } from 'framer-motion'
import type { Universe } from '@/api/types'
import { parseProbability, calculateOverallScore, getUniverseColor, parseMetrics } from '@/utils/reportParser'
import { MetricsChart } from './MetricsChart'
import './ComparisonView.css'

interface ComparisonViewProps {
  universes: Universe[]
}

type ViewMode = 'table' | 'radar' | 'cards'

export const ComparisonView: React.FC<ComparisonViewProps> = ({ universes }) => {
  const [viewMode, setViewMode] = useState<ViewMode>('table')

  // 表格数据
  const tableData = universes.map((universe, index) => {
    const probability = parseProbability(universe.probability)
    const overallScore = calculateOverallScore(universe)
    const metrics = parseMetrics(universe.metrics)
    const colors = getUniverseColor(index)

    return {
      key: index,
      name: universe.name,
      probability,
      overallScore,
      happiness: metrics.happiness || 0,
      financial: metrics.financial || 0,
      growth: metrics.growth || 0,
      stability: metrics.stability || 0,
      description: universe.description,
      colors,
    }
  })

  const columns = [
    {
      title: '宇宙名称',
      dataIndex: 'name',
      key: 'name',
      render: (text: string, record: any) => (
        <span style={{ color: record.colors.primary, fontWeight: 600 }}>{text}</span>
      ),
    },
    {
      title: '实现概率',
      dataIndex: 'probability',
      key: 'probability',
      sorter: (a: any, b: any) => a.probability - b.probability,
      render: (value: number) => `${value}%`,
    },
    {
      title: '综合评分',
      dataIndex: 'overallScore',
      key: 'overallScore',
      sorter: (a: any, b: any) => a.overallScore - b.overallScore,
      render: (value: number) => `${value}/100`,
    },
    {
      title: '幸福感',
      dataIndex: 'happiness',
      key: 'happiness',
      sorter: (a: any, b: any) => a.happiness - b.happiness,
      render: (value: number) => value || '-',
    },
    {
      title: '财务状况',
      dataIndex: 'financial',
      key: 'financial',
      sorter: (a: any, b: any) => a.financial - b.financial,
      render: (value: number) => value || '-',
    },
    {
      title: '成长潜力',
      dataIndex: 'growth',
      key: 'growth',
      sorter: (a: any, b: any) => a.growth - b.growth,
      render: (value: number) => value || '-',
    },
    {
      title: '稳定性',
      dataIndex: 'stability',
      key: 'stability',
      sorter: (a: any, b: any) => a.stability - b.stability,
      render: (value: number) => value || '-',
    },
  ]

  return (
    <div className="comparison-view">
      <div className="comparison-header">
        <h3>对比分析</h3>
        <Radio.Group
          value={viewMode}
          onChange={(e) => setViewMode(e.target.value)}
          buttonStyle="solid"
        >
          <Radio.Button value="table">
            <TableOutlined /> 表格
          </Radio.Button>
          <Radio.Button value="radar">
            <RadarChartOutlined /> 雷达图
          </Radio.Button>
          <Radio.Button value="cards">
            <BarChartOutlined /> 卡片
          </Radio.Button>
        </Radio.Group>
      </div>

      <div className="comparison-content">
        {viewMode === 'table' && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4 }}
          >
            <Table
              dataSource={tableData}
              columns={columns}
              pagination={false}
              className="comparison-table"
            />
          </motion.div>
        )}

        {viewMode === 'radar' && (
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.4 }}
            className="comparison-radar"
          >
            <div className="radar-charts-grid">
              {universes.map((universe, index) => (
                <Card
                  key={index}
                  title={universe.name}
                  className="radar-card"
                  style={{ borderColor: getUniverseColor(index).primary }}
                >
                  <MetricsChart universe={universe} index={index} />
                </Card>
              ))}
            </div>
          </motion.div>
        )}

        {viewMode === 'cards' && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.4 }}
            className="comparison-cards"
          >
            <div className="cards-grid">
              {tableData.map((item, index) => (
                <Card
                  key={index}
                  className="comparison-card"
                  style={{ borderColor: item.colors.primary }}
                >
                  <div className="card-header">
                    <h4 style={{ color: item.colors.primary, margin: 0 }}>
                      {item.name}
                    </h4>
                  </div>
                  <div className="card-metrics">
                    <div className="metric-row">
                      <span>实现概率</span>
                      <Tag color={item.colors.primary}>{item.probability}%</Tag>
                    </div>
                    <div className="metric-row">
                      <span>综合评分</span>
                      <Tag color={item.colors.secondary}>{item.overallScore}/100</Tag>
                    </div>
                    <div className="metric-row">
                      <span>幸福感</span>
                      <span>{item.happiness || '-'}</span>
                    </div>
                    <div className="metric-row">
                      <span>财务状况</span>
                      <span>{item.financial || '-'}</span>
                    </div>
                    <div className="metric-row">
                      <span>成长潜力</span>
                      <span>{item.growth || '-'}</span>
                    </div>
                    <div className="metric-row">
                      <span>稳定性</span>
                      <span>{item.stability || '-'}</span>
                    </div>
                  </div>
                </Card>
              ))}
            </div>
          </motion.div>
        )}
      </div>
    </div>
  )
}

