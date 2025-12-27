import React, { useState } from 'react'
import { Typography, Divider, Tabs, Button, Space } from 'antd'
import { FileTextOutlined, SwapOutlined, DownloadOutlined } from '@ant-design/icons'
import { motion } from 'framer-motion'
import type { ParallelLifeReport } from '@/api/types'
import { UniverseCard } from './UniverseCard'
import { UniverseDetail } from './UniverseDetail'
import { ComparisonView } from './ComparisonView'
import { RecommendationsList } from './RecommendationsList'
import './ReportView.css'

const { Title, Paragraph } = Typography

interface ReportViewProps {
  report: ParallelLifeReport
  onClose?: () => void
}

export const ReportView: React.FC<ReportViewProps> = ({ report, onClose }) => {
  const [selectedUniverse, setSelectedUniverse] = useState<number | null>(null)
  const [detailVisible, setDetailVisible] = useState(false)
  const [activeTab, setActiveTab] = useState('universes')

  const handleViewDetail = (index: number) => {
    setSelectedUniverse(index)
    setDetailVisible(true)
  }

  const handleCompare = () => {
    setActiveTab('comparison')
  }

  const tabItems = [
    {
      key: 'universes',
      label: (
        <span>
          <FileTextOutlined /> 平行宇宙
        </span>
      ),
      children: (
        <div className="universes-grid">
          {report.universes.map((universe, index) => (
            <UniverseCard
              key={index}
              universe={universe}
              index={index}
              onViewDetail={() => handleViewDetail(index)}
              onCompare={handleCompare}
            />
          ))}
        </div>
      ),
    },
    {
      key: 'comparison',
      label: (
        <span>
          <SwapOutlined /> 对比分析
        </span>
      ),
      children: <ComparisonView universes={report.universes} />,
    },
  ]

  return (
    <motion.div
      className="report-view"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
    >
      <div className="report-header">
        <div className="report-title-section">
          <Title level={2} className="report-title">
            {report.title}
          </Title>
          <Paragraph className="report-situation">{report.currentSituation}</Paragraph>
        </div>
        <Space>
          <Button icon={<DownloadOutlined />} style={{ borderRadius: 12 }}>
            导出报告
          </Button>
          {onClose && (
            <Button onClick={onClose} style={{ borderRadius: 12 }}>
              关闭
            </Button>
          )}
        </Space>
      </div>

      <Divider />

      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={tabItems}
        className="report-tabs"
        size="large"
      />

      <Divider />

      <div className="report-comparison-section">
        <Title level={4}>对比分析</Title>
        <Paragraph>{report.comparison}</Paragraph>
      </div>

      <Divider />

      <RecommendationsList recommendations={report.recommendations} />

      {selectedUniverse !== null && (
        <UniverseDetail
          universe={report.universes[selectedUniverse]}
          index={selectedUniverse}
          visible={detailVisible}
          onClose={() => {
            setDetailVisible(false)
            setSelectedUniverse(null)
          }}
        />
      )}
    </motion.div>
  )
}

