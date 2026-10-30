import type { HistoryTaskDetail, HistoryTaskSummary } from '../types/user'
import type { SysResult } from '../types/factcheck'
import { buildApiUrl } from './config'

const BASE_HISTORY_URL = buildApiUrl('/api/v1/history')

function getAuthHeader(): Record<string, string> {
  const token = localStorage.getItem('familydoc_auth_token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function requestJson<T>(url: string, options: RequestInit): Promise<T> {
  const headers = {
    'Content-Type': 'application/json;charset=UTF-8',
    ...getAuthHeader(),
    ...(options.headers || {})
  }

  const response = await fetch(url, { ...options, headers })
  let body: SysResult<T>
  try {
    body = (await response.json()) as SysResult<T>
  } catch {
    throw new Error('服务响应异常')
  }

  if (!body || body.code !== 200) {
    throw new Error(body?.message || '操作失败')
  }
  return body.data
}

export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
}

/**
 * 分页查询云端核验任务历史
 */
export async function fetchUserHistoryTasks(page = 0, size = 20): Promise<PageResult<HistoryTaskSummary>> {
  return requestJson<PageResult<HistoryTaskSummary>>(
    `${BASE_HISTORY_URL}/tasks?page=${page}&size=${size}`,
    { method: 'GET' }
  )
}

/**
 * 查询单次历史核验详情（含完整论断与证据快照）
 */
export async function fetchHistoryTaskDetail(taskId: string): Promise<HistoryTaskDetail> {
  return requestJson<HistoryTaskDetail>(
    `${BASE_HISTORY_URL}/tasks/${encodeURIComponent(taskId)}`,
    { method: 'GET' }
  )
}

/**
 * 删除单条历史记录
 */
export async function deleteHistoryTask(taskId: string): Promise<void> {
  await requestJson(
    `${BASE_HISTORY_URL}/tasks/${encodeURIComponent(taskId)}`,
    { method: 'DELETE' }
  )
}

/**
 * 清空所有云端历史记录
 */
export async function clearAllHistoryTasks(): Promise<void> {
  await requestJson(`${BASE_HISTORY_URL}/tasks`, {
    method: 'DELETE'
  })
}

