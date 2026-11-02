import type {
  FactCheckRequest,
  FactCheckTaskResponse,
  RedFlagResult,
  SysResult
} from '../types/factcheck'
import { setAuthToken, openAuthModal } from '../utils/userState'
import { buildApiUrl } from './config'

/**
 * 家医智核·流言粉碎 后端接口请求服务
 * 对接后端 FactCheckController (/api/v1/factcheck)
 */

const BASE_URL = buildApiUrl('/api/v1/factcheck')

/**
 * 检查浏览器当前是否处于联网状态（对应需求第 5 节：“当前无网络，请联网后再试”）
 */
function ensureOnline(): void {
  if (typeof navigator !== 'undefined' && navigator.onLine === false) {
    throw new Error('当前无网络，请联网后再试')
  }
}

/**
 * 通用 JSON 请求解析与统一错误提取
 */
async function requestJson<T>(url: string, options: RequestInit): Promise<T> {
  ensureOnline()

  const token = typeof localStorage !== 'undefined' ? localStorage.getItem('familydoc_auth_token') : null
  const authHeader: Record<string, string> = token ? { Authorization: `Bearer ${token}` } : {}

  let response: Response
  try {
    response = await fetch(url, {
      ...options,
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        ...authHeader,
        ...(options.headers || {})
      }
    })
  } catch (err) {
    ensureOnline()
    throw new Error('网络连接异常，请检查网络或稍后再试')
  }

  if (response.status === 401) {
    setAuthToken(null)
    openAuthModal('login')
    throw new Error('登录已失效或尚未登录，请先登录')
  }

  let body: SysResult<T>
  try {
    body = (await response.json()) as SysResult<T>
  } catch (err) {
    throw new Error('服务响应格式异常，请稍后再试')
  }

  if (body?.code === 401) {
    setAuthToken(null)
    openAuthModal('login')
    throw new Error(body?.message || '请先登录')
  }

  if (!body || body.code !== 200) {
    throw new Error(body?.message || '请求处理失败，请稍后再试')
  }

  return body.data
}

/**
 * 创建并启动流言核验任务
 * POST /api/v1/factcheck/createTask
 *
 * @param payload 包含待核验文本 text、ignoreHardRedFlag 与 skipCache
 */
export async function createFactCheckTask(
  payload: FactCheckRequest
): Promise<FactCheckTaskResponse> {
  return requestJson<FactCheckTaskResponse>(`${BASE_URL}/createTask`, {
    method: 'POST',
    body: JSON.stringify({
      text: payload.text,
      ignoreHardRedFlag: Boolean(payload.ignoreHardRedFlag),
      skipCache: Boolean(payload.skipCache)
    })
  })
}

/**
 * 实时查询核验任务检索进度与逐条论断状态
 * GET /api/v1/factcheck/tasks/{taskId}
 *
 * @param taskId 任务唯一 ID
 */
export async function fetchFactCheckTaskStatus(taskId: string): Promise<FactCheckTaskResponse> {
  return requestJson<FactCheckTaskResponse>(
    `${BASE_URL}/tasks/${encodeURIComponent(taskId)}`,
    {
      method: 'GET'
    }
  )
}

/**
 * 取消正在进行的核验任务（对应需求 US-01“核验中可取消”）
 * POST /api/v1/factcheck/tasks/{taskId}/cancel
 *
 * @param taskId 任务唯一 ID
 */
export async function cancelFactCheckTask(taskId: string): Promise<FactCheckTaskResponse> {
  return requestJson<FactCheckTaskResponse>(
    `${BASE_URL}/tasks/${encodeURIComponent(taskId)}/cancel`,
    {
      method: 'POST'
    }
  )
}

/**
 * 单条失败论断单独重试（对应需求 US-07“某条失败可单独重试”与 4.10 服务端安全校验）
 * POST /api/v1/factcheck/tasks/{taskId}/claims/{claimId}/retry
 *
 * @param taskId  任务唯一 ID
 * @param claimId 论断唯一 ID（如 c-1）
 */
export async function retrySingleClaimTask(
  taskId: string,
  claimId: string
): Promise<FactCheckTaskResponse> {
  return requestJson<FactCheckTaskResponse>(
    `${BASE_URL}/tasks/${encodeURIComponent(taskId)}/claims/${encodeURIComponent(claimId)}/retry`,
    {
      method: 'POST'
    }
  )
}

/**
 * 调用服务端危急红线扫描接口
 * POST /api/v1/factcheck/redflag/scan
 */
export async function scanRedFlagByServer(
  text: string,
  ignoreHardRedFlag = false
): Promise<RedFlagResult> {
  return requestJson<RedFlagResult>(`${BASE_URL}/redflag/scan`, {
    method: 'POST',
    body: JSON.stringify({ text, ignoreHardRedFlag })
  })
}

/**
 * 上传图片提取文字（腾讯云 OCR 多模态提取）
 * POST /api/v1/factcheck/ocr
 *
 * @param file 图片文件
 */
export async function uploadOcrImage(file: File): Promise<{ text: string }> {
  ensureOnline()

  const token = typeof localStorage !== 'undefined' ? localStorage.getItem('familydoc_auth_token') : null
  const authHeader: Record<string, string> = token ? { Authorization: `Bearer ${token}` } : {}

  const formData = new FormData()
  formData.append('file', file)

  let response: Response
  try {
    response = await fetch(`${BASE_URL}/ocr`, {
      method: 'POST',
      headers: {
        ...authHeader
      },
      body: formData
    })
  } catch (err) {
    ensureOnline()
    throw new Error('网络连接异常，图片上传失败')
  }

  if (response.status === 401) {
    setAuthToken(null)
    openAuthModal('login')
    throw new Error('登录已失效或尚未登录，请先登录')
  }

  let body: SysResult<{ text: string }>
  try {
    body = (await response.json()) as SysResult<{ text: string }>
  } catch (err) {
    throw new Error('服务响应格式异常，请稍后再试')
  }

  if (body?.code === 401) {
    setAuthToken(null)
    openAuthModal('login')
    throw new Error(body?.message || '请先登录')
  }

  if (!body || body.code !== 200) {
    throw new Error(body?.message || '图片文字识别失败，请稍后再试')
  }

  return body.data
}


