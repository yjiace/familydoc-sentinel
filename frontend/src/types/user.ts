/**
 * 用户体系、认证与资源用量相关类型定义
 */

export interface UserProfile {
  id: number
  email: string
  nickname: string
  avatar?: string
  role: string
  status: string
  createdAt: string
}

export interface UserAuthResponse {
  token: string
  userId: number
  email: string
  nickname: string
  avatar?: string
  role: string
}

export interface UserUsage {
  userId: number
  totalTokens: number
  totalPromptTokens: number
  totalCompletionTokens: number
  totalSearchCount: number
  taskCount: number
  updatedAt?: string
}

export interface HistoryTaskSummary {
  id: number
  taskId: string
  originalText: string
  status: string
  summary?: string
  claimCount: number
  redFlagLevel?: string
  tokensUsed?: number
  searchUsed?: number
  createdAt: string
}

export interface HistoryTaskDetail {
  id: number
  taskId: string
  originalText: string
  status: string
  summary?: string
  claimCount: number
  redFlagLevel?: string
  tokensUsed?: number
  searchUsed?: number
  claims?: any[]
  createdAt: string
}

