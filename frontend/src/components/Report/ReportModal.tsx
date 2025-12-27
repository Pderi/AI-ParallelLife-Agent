import React from 'react'
import { Modal } from 'antd'
import { ReportView } from './ReportView'
import type { ParallelLifeReport } from '@/api/types'
import './ReportModal.css'

interface ReportModalProps {
  report: ParallelLifeReport | null
  visible: boolean
  onClose: () => void
}

export const ReportModal: React.FC<ReportModalProps> = ({ report, visible, onClose }) => {
  if (!report) return null

  return (
    <Modal
      title={null}
      open={visible}
      onCancel={onClose}
      footer={null}
      width="90%"
      style={{ top: 20 }}
      className="report-modal"
      destroyOnClose
    >
      <div className="report-modal-content">
        <ReportView report={report} onClose={onClose} />
      </div>
    </Modal>
  )
}

