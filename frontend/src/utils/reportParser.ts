import type { Universe } from '@/api/types'

/**
 * 解析指标字符串，提取各项指标值
 * 格式示例："幸福感：75/100，财务状况：85/100，成长潜力：90/100"
 */
export function parseMetrics(metricsString: string): {
  happiness?: number
  financial?: number
  growth?: number
  stability?: number
  workLifeBalance?: number
} {
  const metrics: Record<string, number> = {}
  
  // 匹配 "指标名：数值/100" 格式
  const patterns = [
    { key: 'happiness', regex: /幸福感[：:]\s*(\d+)/ },
    { key: 'financial', regex: /财务状况[：:]\s*(\d+)/ },
    { key: 'growth', regex: /成长潜力[：:]\s*(\d+)/ },
    { key: 'stability', regex: /稳定性[：:]\s*(\d+)/ },
    { key: 'workLifeBalance', regex: /工作生活平衡[：:]\s*(\d+)/ },
  ]
  
  patterns.forEach(({ key, regex }) => {
    const match = metricsString.match(regex)
    if (match) {
      metrics[key] = parseInt(match[1], 10)
    }
  })
  
  return metrics
}

/**
 * 解析概率字符串，提取百分比数值
 * 格式示例："70%" 或 "概率：70%"
 */
export function parseProbability(probabilityString: string): number {
  const match = probabilityString.match(/(\d+)%/)
  return match ? parseInt(match[1], 10) : 0
}

/**
 * 解析时间线字符串，提取时间节点
 * 格式示例："1年后：高级工程师，薪资15k→25k\n3年后：技术专家..."
 */
export function parseTimeline(timelineString: string): Array<{
  year: string
  description: string
}> {
  const timeline: Array<{ year: string; description: string }> = []
  const lines = timelineString.split('\n').filter(line => line.trim())
  
  lines.forEach(line => {
    // 匹配 "X年后：" 或 "第X年：" 格式
    const match = line.match(/^(\d+年(?:后)?|第\d+年)[：:]\s*(.+)$/)
    if (match) {
      timeline.push({
        year: match[1],
        description: match[2].trim(),
      })
    }
  })
  
  return timeline
}

/**
 * 获取宇宙主题色
 */
export function getUniverseColor(index: number): {
  gradient: string
  primary: string
  secondary: string
} {
  const colors = [
    {
      gradient: 'linear-gradient(135deg, #b8734a 0%, #8b5e3c 100%)',
      primary: '#b8734a',
      secondary: '#8b5e3c',
    },
    {
      gradient: 'linear-gradient(135deg, #4a7c59 0%, #2d5a3d 100%)',
      primary: '#4a7c59',
      secondary: '#2d5a3d',
    },
    {
      gradient: 'linear-gradient(135deg, #5b7b8a 0%, #3d5a6b 100%)',
      primary: '#5b7b8a',
      secondary: '#3d5a6b',
    },
    {
      gradient: 'linear-gradient(135deg, #c4a882 0%, #9a7d5c 100%)',
      primary: '#c4a882',
      secondary: '#9a7d5c',
    },
    {
      gradient: 'linear-gradient(135deg, #8b4d5a 0%, #6b3a45 100%)',
      primary: '#8b4d5a',
      secondary: '#6b3a45',
    },
  ]
  
  return colors[index % colors.length]
}

/**
 * 计算综合评分（基于各项指标的平均值）
 */
export function calculateOverallScore(universe: Universe): number {
  const metrics = parseMetrics(universe.metrics)
  const values = Object.values(metrics).filter(v => v !== undefined) as number[]
  
  if (values.length === 0) return 0
  
  const average = values.reduce((sum, val) => sum + val, 0) / values.length
  return Math.round(average)
}

