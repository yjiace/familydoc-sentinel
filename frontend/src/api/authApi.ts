import type { UserAuthResponse, UserProfile, UserUsage } from '../types/user'
import type { SysResult } from '../types/factcheck'
import { buildApiUrl } from './config'

const BASE_AUTH_URL = buildApiUrl('/api/v1/auth')
const BASE_USER_URL = buildApiUrl('/api/v1/user')

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
    throw new Error('服务响应异常，请稍后再试')
  }

  if (!body || body.code !== 200) {
    throw new Error(body?.message || '操作失败')
  }
  return body.data
}

/**
 * 发送邮箱验证码
 */
export async function sendEmailCode(email: string, bizType: 'REGISTER' | 'RESET_PWD' = 'REGISTER'): Promise<void> {
  await requestJson(`${BASE_AUTH_URL}/send-code`, {
    method: 'POST',
    body: JSON.stringify({ email, bizType })
  })
}

/**
 * 邮箱+验证码+密码注册
 */
export async function registerWithEmail(payload: {
  email: string
  code: string
  password: string
  nickname?: string
}): Promise<UserAuthResponse> {
  return requestJson<UserAuthResponse>(`${BASE_AUTH_URL}/register`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

/**
 * 获取纯数字图形验证码
 */
export async function fetchCaptcha(): Promise<{ captchaKey: string; imageBase64: string }> {
  return requestJson<{ captchaKey: string; imageBase64: string }>(`${BASE_AUTH_URL}/captcha`, {
    method: 'GET'
  })
}

/**
 * 邮箱+密码+图形验证码登录
 */
export async function loginWithEmail(payload: {
  email: string
  password: string
  captchaKey: string
  captchaCode: string
}): Promise<UserAuthResponse> {
  return requestJson<UserAuthResponse>(`${BASE_AUTH_URL}/login`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

/**
 * 修改密码（已登录用户）
 */
export async function changePassword(payload: {
  oldPassword: string
  newPassword: string
}): Promise<void> {
  await requestJson(`${BASE_AUTH_URL}/change-password`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

/**
 * 重置密码
 */
export async function resetPasswordWithCode(payload: {
  email: string
  code: string
  newPassword: string
}): Promise<void> {
  await requestJson(`${BASE_AUTH_URL}/reset-password`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

/**
 * 获取当前登录用户信息
 */
export async function fetchCurrentUser(): Promise<UserProfile> {
  return requestJson<UserProfile>(`${BASE_AUTH_URL}/me`, {
    method: 'GET'
  })
}

/**
 * 获取当前用户 Token 与元宝搜索用量概览
 */
export async function fetchUserUsage(): Promise<UserUsage> {
  return requestJson<UserUsage>(`${BASE_USER_URL}/usage`, {
    method: 'GET'
  })
}

/**
 * 更新用户昵称或头像
 */
export async function updateUserProfile(payload: {
  nickname?: string
  avatar?: string
}): Promise<UserProfile> {
  return requestJson<UserProfile>(`${BASE_AUTH_URL}/profile`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  })
}

