import { ref, computed } from 'vue'
import type { UserProfile, UserUsage } from '../types/user'
import { fetchCurrentUser, fetchUserUsage } from '../api/authApi'

const TOKEN_KEY = 'familydoc_auth_token'

export type AuthModalMode = 'login' | 'register' | 'forgot' | 'change_password'

export const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
export const currentUser = ref<UserProfile | null>(null)
export const userUsage = ref<UserUsage | null>(null)
export const isAuthModalOpen = ref<boolean>(false)
export const authModalInitialMode = ref<AuthModalMode>('login')

export const isLoggedIn = computed(() => Boolean(token.value && currentUser.value))

/**
 * 设置 Token 并持久化
 */
export function setAuthToken(newToken: string | null): void {
  if (newToken) {
    token.value = newToken
    localStorage.setItem(TOKEN_KEY, newToken)
  } else {
    token.value = ''
    currentUser.value = null
    userUsage.value = null
    localStorage.removeItem(TOKEN_KEY)
  }
}

/**
 * 刷新当前用户信息与资源用量统计
 */
export async function refreshUserData(): Promise<void> {
  if (!token.value) {
    currentUser.value = null
    userUsage.value = null
    return
  }

  try {
    const profile = await fetchCurrentUser()
    currentUser.value = profile
    try {
      const usage = await fetchUserUsage()
      userUsage.value = usage
    } catch {
      // 容错忽略
    }
  } catch (err) {
    // Token 已失效
    setAuthToken(null)
  }
}

/**
 * 打开认证弹窗（支持登录、注册、找回密码、修改密码）
 */
export function openAuthModal(mode: AuthModalMode = 'login'): void {
  authModalInitialMode.value = mode
  isAuthModalOpen.value = true
}

/**
 * 关闭登录/注册弹窗
 */
export function closeAuthModal(): void {
  isAuthModalOpen.value = false
}

/**
 * 安全退出登录
 */
export function logout(): void {
  setAuthToken(null)
}

// 页面初始化时若有 Token 则自动恢复
if (token.value) {
  refreshUserData()
}

