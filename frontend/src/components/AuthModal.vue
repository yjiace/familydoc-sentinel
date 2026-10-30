<template>
  <div v-if="isOpen" class="auth-modal-overlay" @click.self="handleClose">
    <div class="auth-modal-card" role="dialog" aria-modal="true">
      <!-- 头部：标题与关闭按钮 -->
      <div class="modal-header">
        <div class="header-title-box">
          <div class="brand-badge-mini" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none">
              <path d="M12 2L4 5.6V11.8C4 17.2 7.4 22.1 12 23.8C16.6 22.1 20 17.2 20 11.8V5.6L12 2Z" fill="#2563EB" />
              <path d="M9 12.2L11.1 14.3L15.4 10" stroke="#FFFFFF" stroke-width="2" stroke-linecap="round" />
            </svg>
          </div>
          <h2 class="modal-title">{{ modalTitle }}</h2>
        </div>
        <button class="close-btn" type="button" @click="handleClose" aria-label="关闭弹窗">
          <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="2" fill="none">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>

      <!-- 标签页切换：仅在未登录且不是修改密码时显示（登录 / 注册） -->
      <div v-if="currentMode === 'login' || currentMode === 'register'" class="tab-switch-row">
        <button
          type="button"
          class="tab-btn"
          :class="{ active: currentMode === 'login' }"
          @click="switchMode('login')"
        >
          账号登录
        </button>
        <button
          type="button"
          class="tab-btn"
          :class="{ active: currentMode === 'register' }"
          @click="switchMode('register')"
        >
          新用户注册
        </button>
      </div>

      <!-- 表单区域 -->
      <form class="auth-form" @submit.prevent="handleSubmit">
        <!-- 1. 邮箱输入（修改密码模式除外） -->
        <div v-if="currentMode !== 'change_password'" class="form-group">
          <label class="form-label" for="auth-email">电子邮箱</label>
          <div class="input-wrapper">
            <svg class="input-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
              <polyline points="22,6 12,13 2,6" />
            </svg>
            <input
              id="auth-email"
              v-model="email"
              type="email"
              class="form-input"
              placeholder="请输入您的邮箱地址"
              autocomplete="email"
              required
            />
          </div>
        </div>

        <!-- 2. 邮箱验证码输入（注册与找回密码模式） -->
        <div v-if="currentMode === 'register' || currentMode === 'forgot'" class="form-group">
          <label class="form-label" for="auth-code">邮箱验证码</label>
          <div class="code-input-row">
            <div class="input-wrapper flex-1">
              <svg class="input-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
              <input
                id="auth-code"
                v-model="code"
                type="text"
                maxlength="6"
                class="form-input"
                placeholder="6 位数字验证码"
                required
              />
            </div>
            <button
              type="button"
              class="send-code-btn"
              :disabled="countdown > 0 || isSendingCode || !isEmailValid"
              @click="handleSendCode"
            >
              <span v-if="isSendingCode">发送中...</span>
              <span v-else-if="countdown > 0">{{ countdown }}s 后重试</span>
              <span v-else>获取验证码</span>
            </button>
          </div>
        </div>

        <!-- 3. 注册模式下的昵称（选填） -->
        <div v-if="currentMode === 'register'" class="form-group">
          <label class="form-label" for="auth-nickname">昵称（选填）</label>
          <div class="input-wrapper">
            <svg class="input-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
            <input
              id="auth-nickname"
              v-model="nickname"
              type="text"
              maxlength="20"
              class="form-input"
              placeholder="给自己起个好听的昵称"
            />
          </div>
        </div>

        <!-- 4. 修改密码模式下的原密码输入 -->
        <div v-if="currentMode === 'change_password'" class="form-group">
          <label class="form-label" for="auth-old-pwd">当前原密码</label>
          <div class="input-wrapper">
            <svg class="input-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
              <path d="M7 11V7a5 5 0 0 1 10 0v4" />
            </svg>
            <input
              id="auth-old-pwd"
              v-model="oldPassword"
              :type="showPassword ? 'text' : 'password'"
              class="form-input"
              placeholder="请输入当前使用的原密码"
              required
            />
          </div>
        </div>

        <!-- 5. 密码输入（登录/注册/重置/改密） -->
        <div class="form-group">
          <div class="label-row">
            <label class="form-label" for="auth-pwd">
              {{ currentMode === 'login' ? '登录密码' : currentMode === 'change_password' ? '新密码' : '设置密码' }}
            </label>
            <button
              v-if="currentMode === 'login'"
              type="button"
              class="forgot-pwd-link"
              @click="switchMode('forgot')"
            >
              忘记密码？
            </button>
            <span v-else class="pwd-tip">至少 6 位字符</span>
          </div>
          <div class="input-wrapper">
            <svg class="input-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
              <path d="M7 11V7a5 5 0 0 1 10 0v4" />
            </svg>
            <input
              id="auth-pwd"
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              class="form-input"
              :placeholder="
                currentMode === 'login'
                  ? '请输入登录密码'
                  : currentMode === 'register'
                  ? '请设置登录密码（至少 6 位）'
                  : '请输入新密码（至少 6 位）'
              "
              autocomplete="current-password"
              required
            />
            <button
              type="button"
              class="pwd-toggle-btn"
              @click="showPassword = !showPassword"
              aria-label="显示/隐藏密码"
            >
              <svg v-if="showPassword" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
              <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                <line x1="1" y1="1" x2="23" y2="23" />
              </svg>
            </button>
          </div>
        </div>

        <!-- 5.1 确认密码输入（注册/找回密码/修改密码时均需要二次确认） -->
        <div v-if="currentMode !== 'login'" class="form-group">
          <div class="label-row">
            <label class="form-label" for="auth-confirm-pwd">
              {{ currentMode === 'register' ? '确认密码' : '确认新密码' }}
            </label>
          </div>
          <div class="input-wrapper">
            <svg class="input-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
              <path d="M7 11V7a5 5 0 0 1 10 0v4" />
            </svg>
            <input
              id="auth-confirm-pwd"
              v-model="confirmPassword"
              :type="showConfirmPassword ? 'text' : 'password'"
              class="form-input"
              placeholder="请再次输入密码以确认"
              required
            />
            <button
              type="button"
              class="pwd-toggle-btn"
              @click="showConfirmPassword = !showConfirmPassword"
              aria-label="显示/隐藏确认密码"
            >
              <svg v-if="showConfirmPassword" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
              <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                <line x1="1" y1="1" x2="23" y2="23" />
              </svg>
            </button>
          </div>
        </div>

        <!-- 6. 登录模式专用的纯数字图形验证码 -->
        <div v-if="currentMode === 'login'" class="form-group">
          <label class="form-label" for="auth-captcha">图形验证码</label>
          <div class="captcha-input-row">
            <div class="input-wrapper flex-1">
              <svg class="input-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="2" y="2" width="20" height="20" rx="5" ry="5" />
                <path d="M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z" />
              </svg>
              <input
                id="auth-captcha"
                v-model="captchaCode"
                type="text"
                maxlength="4"
                class="form-input"
                placeholder="4 位纯数字"
                required
              />
            </div>
            <div class="captcha-img-box" @click="refreshCaptchaImg" title="看不清？点击更换一张">
              <img v-if="captchaImg" :src="captchaImg" alt="图形验证码" class="captcha-img" />
              <span v-else class="captcha-loading">加载中...</span>
            </div>
          </div>
        </div>

        <!-- 错误提示条 -->
        <div v-if="errorMessage" class="error-banner">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          <span>{{ errorMessage }}</span>
        </div>

        <!-- 成功提示条 -->
        <div v-if="successMessage" class="success-banner">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
            <polyline points="22 4 12 14.01 9 11.01" />
          </svg>
          <span>{{ successMessage }}</span>
        </div>

        <!-- 提交按钮 -->
        <button type="submit" class="submit-auth-btn" :disabled="isSubmitting">
          <span v-if="isSubmitting" class="loading-spinner"></span>
          <span>{{ submitBtnText }}</span>
        </button>

        <!-- 找回密码模式下的“返回登录”链接 -->
        <div v-if="currentMode === 'forgot'" class="switch-back-row">
          <button type="button" class="back-login-link" @click="switchMode('login')">
            ← 想起密码了？返回登录
          </button>
        </div>

        <p class="auth-disclaimer">
          登录或注册即代表您同意本平台的健康信息科普免责声明
        </p>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import {
  isAuthModalOpen as isOpen,
  authModalInitialMode,
  closeAuthModal,
  setAuthToken,
  refreshUserData,
  type AuthModalMode
} from '../utils/userState'
import {
  sendEmailCode,
  registerWithEmail,
  loginWithEmail,
  fetchCaptcha,
  resetPasswordWithCode,
  changePassword
} from '../api/authApi'

const currentMode = ref<AuthModalMode>('login')

const email = ref<string>('')
const code = ref<string>('')
const nickname = ref<string>('')
const password = ref<string>('')
const confirmPassword = ref<string>('')
const oldPassword = ref<string>('')
const showPassword = ref<boolean>(false)
const showConfirmPassword = ref<boolean>(false)

// 图形验证码状态
const captchaKey = ref<string>('')
const captchaImg = ref<string>('')
const captchaCode = ref<string>('')

// 邮箱验证码状态
const isSendingCode = ref<boolean>(false)
const countdown = ref<number>(0)
let timer: any = null

const isSubmitting = ref<boolean>(false)
const errorMessage = ref<string>('')
const successMessage = ref<string>('')

const isEmailValid = computed(() => {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim())
})

const modalTitle = computed(() => {
  switch (currentMode.value) {
    case 'register':
      return '加入家医智核'
    case 'forgot':
      return '找回密码'
    case 'change_password':
      return '修改登录密码'
    default:
      return '欢迎登录'
  }
})

const submitBtnText = computed(() => {
  switch (currentMode.value) {
    case 'register':
      return '立即注册并登录'
    case 'forgot':
      return '立即重置密码'
    case 'change_password':
      return '确认修改密码'
    default:
      return '立即登录'
  }
})

watch(authModalInitialMode, (val) => {
  currentMode.value = val || 'login'
})

watch(isOpen, (val) => {
  if (val) {
    currentMode.value = authModalInitialMode.value || 'login'
    errorMessage.value = ''
    successMessage.value = ''
    password.value = ''
    confirmPassword.value = ''
    oldPassword.value = ''
    captchaCode.value = ''
    if (currentMode.value === 'login') {
      refreshCaptchaImg()
    }
  }
})

function switchMode(mode: AuthModalMode): void {
  currentMode.value = mode
  errorMessage.value = ''
  successMessage.value = ''
  password.value = ''
  confirmPassword.value = ''
  oldPassword.value = ''
  captchaCode.value = ''
  if (mode === 'login') {
    refreshCaptchaImg()
  }
}

async function refreshCaptchaImg(): Promise<void> {
  captchaImg.value = ''
  captchaKey.value = ''
  try {
    const res = await fetchCaptcha()
    captchaKey.value = res.captchaKey
    captchaImg.value = res.imageBase64
  } catch (e: any) {
    errorMessage.value = '获取图形验证码失败，请稍后再试'
  }
}

function handleClose(): void {
  closeAuthModal()
}

/**
 * 发送邮箱验证码
 */
async function handleSendCode(): Promise<void> {
  if (!isEmailValid.value) {
    errorMessage.value = '请输入正确的电子邮箱'
    return
  }

  errorMessage.value = ''
  isSendingCode.value = true

  const bizType = currentMode.value === 'forgot' ? 'RESET_PWD' : 'REGISTER'
  try {
    await sendEmailCode(email.value.trim(), bizType)
    successMessage.value = '验证码已发送至您的邮箱，请在 5 分钟内填写'
    countdown.value = 60
    if (timer) clearInterval(timer)
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (err: any) {
    errorMessage.value = err.message || '发送验证码失败'
  } finally {
    isSendingCode.value = false
  }
}

/**
 * 统一提交处理
 */
async function handleSubmit(): Promise<void> {
  errorMessage.value = ''
  successMessage.value = ''

  if (currentMode.value !== 'change_password' && !isEmailValid.value) {
    errorMessage.value = '请输入有效的电子邮箱'
    return
  }

  if (password.value.length < 6) {
    errorMessage.value = '密码长度至少为 6 位'
    return
  }

  // 非登录模式下（注册、找回密码、修改密码）强制要求输入两次密码并验证一致性
  if (currentMode.value !== 'login') {
    if (!confirmPassword.value) {
      errorMessage.value = '请再次输入密码以确认'
      return
    }
    if (password.value !== confirmPassword.value) {
      errorMessage.value = '两次输入的密码不一致，请重新输入'
      return
    }
  }

  isSubmitting.value = true

  try {
    if (currentMode.value === 'login') {
      if (!captchaCode.value.trim()) {
        errorMessage.value = '请输入图形验证码'
        isSubmitting.value = false
        return
      }
      const authRes = await loginWithEmail({
        email: email.value.trim(),
        password: password.value,
        captchaKey: captchaKey.value,
        captchaCode: captchaCode.value.trim()
      })
      setAuthToken(authRes.token)
      await refreshUserData()
      handleClose()
    } else if (currentMode.value === 'register') {
      if (!code.value.trim()) {
        errorMessage.value = '请输入邮箱验证码'
        isSubmitting.value = false
        return
      }
      const authRes = await registerWithEmail({
        email: email.value.trim(),
        code: code.value.trim(),
        password: password.value,
        nickname: nickname.value.trim() || undefined
      })
      setAuthToken(authRes.token)
      await refreshUserData()
      handleClose()
    } else if (currentMode.value === 'forgot') {
      if (!code.value.trim()) {
        errorMessage.value = '请输入邮箱验证码'
        isSubmitting.value = false
        return
      }
      await resetPasswordWithCode({
        email: email.value.trim(),
        code: code.value.trim(),
        newPassword: password.value
      })
      successMessage.value = '密码重置成功，请使用新密码登录'
      setTimeout(() => {
        switchMode('login')
      }, 1200)
    } else if (currentMode.value === 'change_password') {
      if (!oldPassword.value) {
        errorMessage.value = '请输入当前原密码'
        isSubmitting.value = false
        return
      }
      await changePassword({
        oldPassword: oldPassword.value,
        newPassword: password.value
      })
      // 修改密码后，自动退出当前登录态并引导用户使用新密码重新登录
      setAuthToken(null)
      successMessage.value = '密码修改成功，请使用新密码重新登录'
      setTimeout(() => {
        switchMode('login')
      }, 1200)
    }
  } catch (err: any) {
    errorMessage.value = err.message || '操作失败，请稍后再试'
    if (currentMode.value === 'login') {
      refreshCaptchaImg()
      captchaCode.value = ''
    }
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped>
.auth-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(5px);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  animation: fadeIn 0.15s ease-out;
}

.auth-modal-card {
  width: 100%;
  max-width: 410px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 20px 45px -10px rgba(15, 23, 42, 0.25);
  padding: 24px 28px;
  animation: scaleUp 0.18s ease-out;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.header-title-box {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-badge-mini {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #eff6ff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-title {
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.close-btn {
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}

.close-btn:hover {
  background: #f1f5f9;
  color: #334155;
}

.tab-switch-row {
  display: flex;
  background: #f1f5f9;
  padding: 3px;
  border-radius: 10px;
  margin-bottom: 20px;
}

.tab-btn {
  flex: 1;
  border: none;
  background: transparent;
  padding: 7px 0;
  font-size: 13.5px;
  font-weight: 600;
  color: #64748b;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.tab-btn.active {
  background: #ffffff;
  color: #2563eb;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.form-label {
  font-size: 12.5px;
  font-weight: 700;
  color: #334155;
}

.forgot-pwd-link {
  background: transparent;
  border: none;
  color: #2563eb;
  font-size: 11.5px;
  cursor: pointer;
  font-weight: 600;
  padding: 0;
}

.forgot-pwd-link:hover {
  text-decoration: underline;
}

.pwd-tip {
  font-size: 11px;
  color: #94a3b8;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.flex-1 {
  flex: 1;
}

.input-icon {
  position: absolute;
  left: 12px;
  color: #94a3b8;
  pointer-events: none;
}

.form-input {
  width: 100%;
  height: 40px;
  padding: 0 36px 0 36px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 13px;
  color: #0f172a;
  background: #ffffff;
  transition: all 0.15s ease;
}

.form-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}

.pwd-toggle-btn {
  position: absolute;
  right: 10px;
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
}

.pwd-toggle-btn:hover {
  color: #475569;
}

.code-input-row,
.captcha-input-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.send-code-btn {
  height: 40px;
  padding: 0 14px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
}

.send-code-btn:hover:not(:disabled) {
  background: #dbeafe;
}

.send-code-btn:disabled {
  background: #f1f5f9;
  border-color: #e2e8f0;
  color: #94a3b8;
  cursor: not-allowed;
}

.captcha-img-box {
  width: 115px;
  height: 40px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #cbd5e1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  user-select: none;
}

.captcha-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-loading {
  font-size: 11px;
  color: #94a3b8;
}

.error-banner {
  padding: 8px 12px;
  border-radius: 8px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.success-banner {
  padding: 8px 12px;
  border-radius: 8px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #16a34a;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.submit-auth-btn {
  margin-top: 4px;
  height: 42px;
  border: none;
  border-radius: 9px;
  background: #2563eb;
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.15s ease;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
}

.submit-auth-btn:hover:not(:disabled) {
  background: #1d4ed8;
}

.submit-auth-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.switch-back-row {
  display: flex;
  justify-content: center;
}

.back-login-link {
  background: transparent;
  border: none;
  color: #64748b;
  font-size: 12px;
  cursor: pointer;
  padding: 4px;
}

.back-login-link:hover {
  color: #2563eb;
}

.auth-disclaimer {
  font-size: 11px;
  color: #94a3b8;
  text-align: center;
  margin: 6px 0 0;
  line-height: 1.4;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes scaleUp {
  from { transform: scale(0.96); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}
</style>
