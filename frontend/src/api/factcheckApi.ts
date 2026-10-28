import type {
  FactCheckRequest,
  FactCheckTaskResponse,
  RedFlagResult,
  SysResult
} from '../types/factcheck'

/**
 * 家医智核·流言粉碎 后端接口请求服务
 * 对接后端 FactCheckController (/api/v1/factcheck)
 */

const BASE_URL = '/api/v1/factcheck'

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

  let response: Response
  try {
    response = await fetch(url, {
      ...options,
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        ...(options.headers || {})
      }
    })
  } catch (err) {
    ensureOnline()
    throw new Error('网络连接异常，请检查网络或稍后再试')
  }

  let body: SysResult<T>
  try {
    body = (await response.json()) as SysResult<T>
  } catch (err) {
    throw new Error('服务响应格式异常，请稍后再试')
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

