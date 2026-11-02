<template>
  <div class="page-wrapper">
    <!-- 顶部品牌横幅 -->
    <HeroBanner />

    <!-- 主体双栏工作区（视口自适应） -->
    <main class="workspace-container">
      <!-- 左栏：输入与示例面板 -->
      <InputPanel
        v-model="inputText"
        :loading="isPolling"
        :validationError="inputValidationError"
        @submit="handleTriggerVerify(false, false)"
        @cancel="handleCancelTask"
        @clear="handleClearInput"
      />

      <!-- 右栏：核验进度与结果面板 -->
      <section class="result-panel-card">
        <!-- 1. 顶部整篇核验摘要栏（始终固定在面板顶部，右上角常驻“历史记录”入口） -->
        <ArticleSummaryBar
          :summaryText="summaryBannerText"
          :isCacheHit="isShowingCachedResult"
          :loading="isPolling"
          :canGeneratePoster="displayedClaims.length > 0"
          @reverify="handleTriggerVerify(ignoreHardRedFlagState, true)"
          @openHistory="openHistoryDrawer"
          @openPoster="isSharePosterOpen = true"
        />

        <!-- 内部可滚动内容容器 -->
        <div class="result-scroll-content">
          <!-- 2. 危急红线置顶警报 -->
          <RedFlagAlert
            :redFlag="activeRedFlag"
            @continueAsArticle="handleContinueFromHardRedFlag"
          />

          <!-- 3. 实时检索与核验进度看板 -->
          <ProgressTracker
            v-if="currentTask && currentTask.status === 'PROCESSING'"
            :task="currentTask"
          />

          <!-- 4. 全局提示框（纯矢量 SVG） -->
          <div
            v-if="noticeBannerText && (!activeRedFlag || activeRedFlag.level !== 'hard')"
            class="notice-alert-box"
            role="status"
          >
            <svg class="notice-svg-icon" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="16" x2="12" y2="12" />
              <line x1="12" y1="8" x2="12.01" y2="8" />
            </svg>
            <p class="notice-text">{{ noticeBannerText }}</p>
          </div>

          <!-- 5. 逐条核验论断卡片列表 -->
          <div v-if="displayedClaims.length > 0" class="claims-list-container">
            <ClaimCard
              v-for="claim in displayedClaims"
              :key="claim.claimId"
              :claim="claim"
              :retrying="retryingClaimId === claim.claimId"
              @retry="handleRetrySingleClaim"
              @openPoster="isSharePosterOpen = true"
            />
          </div>

          <!-- 6. 初始引导空状态（高质感矢量插图，无原始 emoji） -->
          <div
            v-else-if="!isPolling && !noticeBannerText && (!activeRedFlag || activeRedFlag.level === 'none')"
            class="empty-guide-state"
          >
            <div class="empty-illustration-box" aria-hidden="true">
              <svg viewBox="0 0 48 48" width="36" height="36" fill="none">
                <circle cx="21" cy="21" r="14" stroke="#2563EB" stroke-width="3.5" />
                <path d="M31 31L42 42" stroke="#2563EB" stroke-width="4" stroke-linecap="round" />
                <circle cx="21" cy="21" r="9" fill="#DBEAFE" fill-opacity="0.6" />
                <path d="M17 21H25M21 17V25" stroke="#2563EB" stroke-width="2.2" stroke-linecap="round" />
              </svg>
            </div>
            <h3 class="empty-title">粘贴健康流言，逐条查证权威医学出处</h3>
            <p class="empty-desc">
              支持粘贴长文或多条养生说法（2000 字以内）。系统自动拆解论断、检索卫健委与权威医学指南，并为您生成核验结论与出处原文。
            </p>
            <div class="stamp-legend-row">
              <span class="legend-chip chip-supported">有依据 · 证据相符</span>
              <span class="legend-chip chip-misleading">有误导 · 与依据矛盾</span>
              <span class="legend-chip chip-conditional">看情况 · 条件受限</span>
              <span class="legend-chip chip-unverified">没查实 · 无法核实</span>
              <span class="legend-chip chip-doctor">问医生 · 请咨询医生</span>
            </div>
          </div>

          <!-- 7. 未纳入核验的内容及原因折叠清单 -->
          <UnverifiedCollapse :items="currentTask?.unverifiedItems" />
        </div>

        <!-- 8. 面板底部常驻免责声明 -->
        <footer class="result-footer-disclaimer">
          {{
            currentTask?.disclaimer ||
            '比对信息仅供健康参考，不可作为处方或诊疗依据；内容由 AI 辅助生成，非官方结论。'
          }}
        </footer>
      </section>
    </main>

    <!-- 首次使用知情须知确认弹窗 -->
    <ConsentModal
      :visible="showConsentModal"
      @confirm="handleConfirmConsent"
      @cancel="showConsentModal = false"
    />

    <!-- 核验历史记录抽屉（统一使用 PostgreSQL 云端数据库持久化存储） -->
    <LocalHistoryDrawer
      :visible="showHistoryDrawer"
      @close="showHistoryDrawer = false"
      @selectCloudTask="handleSelectCloudTask"
    />

    <!-- 用户登录/注册一体化弹窗 -->
    <AuthModal />

    <!-- 全量辟谣关怀长图海报弹窗（汇聚本次核验的所有说法与综述） -->
    <SharePosterModal
      v-model:visible="isSharePosterOpen"
      :summaryText="summaryBannerText"
      :claims="displayedClaims"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import HeroBanner from './components/HeroBanner.vue'
import InputPanel from './components/InputPanel.vue'
import RedFlagAlert from './components/RedFlagAlert.vue'
import ProgressTracker from './components/ProgressTracker.vue'
import ArticleSummaryBar from './components/ArticleSummaryBar.vue'
import ClaimCard from './components/ClaimCard.vue'
import UnverifiedCollapse from './components/UnverifiedCollapse.vue'
import ConsentModal from './components/ConsentModal.vue'
import LocalHistoryDrawer from './components/LocalHistoryDrawer.vue'
import AuthModal from './components/AuthModal.vue'
import SharePosterModal from './components/SharePosterModal.vue'
import { refreshUserData, isLoggedIn, openAuthModal } from './utils/userState'

import {
  cancelFactCheckTask,
  createFactCheckTask,
  fetchFactCheckTaskStatus,
  retrySingleClaimTask
} from './api/factcheckApi'
import type {
  ClaimItemDto,
  FactCheckTaskResponse,
  RedFlagResult
} from './types/factcheck'
import { scanRedFlagClient } from './utils/clientRedFlagScanner'
import {
  hasConfirmedConsent,
  markConsentConfirmed,
  preprocessInputText
} from './utils/localCacheManager'

const inputText = ref('听说经常吃黑木耳能软化血管降低血压，是真的吗？')
const inputValidationError = ref('')
const isPolling = ref(false)
const retryingClaimId = ref<string | null>(null)
const ignoreHardRedFlagState = ref(false)
const isLocalCacheHit = ref(false)
const isSharePosterOpen = ref(false)

const currentTask = ref<FactCheckTaskResponse | null>(null)
const clientRedFlag = ref<RedFlagResult | null>(null)
const globalErrorMessage = ref('')

const showConsentModal = ref(false)
const pendingVerifyOptions = ref<{ ignoreHardRedFlag: boolean; skipCache: boolean } | null>(null)
const showHistoryDrawer = ref(false)

let pollTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  refreshUserData()
})

watch(inputText, () => {
  if (inputValidationError.value) {
    inputValidationError.value = ''
  }
})

// 监听登录态变化：当用户退出登录时，彻底清空当前页面上的全部查询数据与任务状态
watch(isLoggedIn, (logged) => {
  if (!logged) {
    stopPolling()
    isPolling.value = false
    currentTask.value = null
    clientRedFlag.value = null
    globalErrorMessage.value = ''
    retryingClaimId.value = null
    isLocalCacheHit.value = false
    showHistoryDrawer.value = false
    inputValidationError.value = ''
    inputText.value = ''
  }
})

const activeRedFlag = computed<RedFlagResult | null>(() => {
  if (currentTask.value?.redFlag && currentTask.value.redFlag.level !== 'none') {
    return currentTask.value.redFlag
  }
  return clientRedFlag.value
})

const isShowingCachedResult = computed(() => {
  return Boolean(isLocalCacheHit.value || currentTask.value?.cacheHit)
})

const summaryBannerText = computed(() => {
  if (activeRedFlag.value?.level === 'hard') {
    return '已触发危急重症红线拦截，请优先就医或拨打 120。'
  }
  if (currentTask.value?.articleSummary) {
    return currentTask.value.articleSummary
  }
  if (isPolling.value) {
    return currentTask.value?.stageDescription || '正在拆解文中健康说法并检索权威医学出处...'
  }
  return '请在左侧输入待核验说法，点击“开始核验”获取逐条查证结果。'
})

const noticeBannerText = computed(() => {
  if (globalErrorMessage.value) {
    return globalErrorMessage.value
  }
  if (
    currentTask.value &&
    (currentTask.value.status === 'BLOCKED' ||
      currentTask.value.status === 'FAILED' ||
      currentTask.value.status === 'CANCELLED')
  ) {
    return currentTask.value.noticeMessage || currentTask.value.stageDescription || ''
  }
  return ''
})

const displayedClaims = computed<ClaimItemDto[]>(() => {
  return currentTask.value?.claims || []
})

async function handleTriggerVerify(ignoreHardRedFlag = false, skipCache = false) {
  // 必须登录后才能使用本平台
  if (!isLoggedIn.value) {
    openAuthModal('login')
    return
  }

  inputValidationError.value = ''
  globalErrorMessage.value = ''

  const normalizedText = preprocessInputText(inputText.value)
  if (!normalizedText || normalizedText.length > 2000) {
    inputValidationError.value = '请输入 2000 字以内的文字'
    return
  }

  if (!hasConfirmedConsent()) {
    pendingVerifyOptions.value = { ignoreHardRedFlag, skipCache }
    showConsentModal.value = true
    return
  }

  ignoreHardRedFlagState.value = ignoreHardRedFlag
  stopPolling()

  const localRedFlag = scanRedFlagClient(normalizedText, ignoreHardRedFlag)
  clientRedFlag.value = localRedFlag
  if (localRedFlag.level === 'hard') {
    currentTask.value = null
    isLocalCacheHit.value = false
    return
  }

  isLocalCacheHit.value = false
  isPolling.value = true

  try {
    const initialTask = await createFactCheckTask({
      text: normalizedText,
      ignoreHardRedFlag,
      skipCache
    })
    currentTask.value = initialTask

    if (initialTask.status === 'PROCESSING') {
      startPolling(initialTask.taskId, normalizedText)
    } else {
      isPolling.value = false
      refreshUserData()
    }
  } catch (err: any) {
    isPolling.value = false
    globalErrorMessage.value = err?.message || '核验请求失败，请稍后再试'
    refreshUserData()
  }
}

function handleConfirmConsent() {
  markConsentConfirmed()
  showConsentModal.value = false
  const opts = pendingVerifyOptions.value || { ignoreHardRedFlag: false, skipCache: false }
  pendingVerifyOptions.value = null
  handleTriggerVerify(opts.ignoreHardRedFlag, opts.skipCache)
}

function handleContinueFromHardRedFlag() {
  handleTriggerVerify(true, false)
}

function startPolling(taskId: string, rawTextForCache: string) {
  stopPolling()
  isPolling.value = true

  pollTimer = setInterval(async () => {
    try {
      const latest = await fetchFactCheckTaskStatus(taskId)
      currentTask.value = latest

      if (latest.status !== 'PROCESSING') {
        stopPolling()
        isPolling.value = false
        retryingClaimId.value = null
        refreshUserData()
      }
    } catch (err: any) {
      stopPolling()
      isPolling.value = false
      retryingClaimId.value = null
      globalErrorMessage.value = err?.message || '同步核验进度失败，请稍后重试'
    }
  }, 800)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function handleCancelTask() {
  stopPolling()
  isPolling.value = false
  const taskId = currentTask.value?.taskId
  if (!taskId) return

  try {
    const cancelledTask = await cancelFactCheckTask(taskId)
    currentTask.value = cancelledTask
    refreshUserData()
  } catch (err: any) {
    globalErrorMessage.value = err?.message || '已取消当前核验任务'
  }
}

async function handleRetrySingleClaim(claimId: string) {
  if (!isLoggedIn.value) {
    openAuthModal('login')
    return
  }

  const taskId = currentTask.value?.taskId
  if (!taskId || !claimId) return

  globalErrorMessage.value = ''
  retryingClaimId.value = claimId

  try {
    const updatedTask = await retrySingleClaimTask(taskId, claimId)
    currentTask.value = updatedTask
    startPolling(taskId, inputText.value)
  } catch (err: any) {
    retryingClaimId.value = null
    globalErrorMessage.value = err?.message || '单条重试请求失败，请稍后再试'
    refreshUserData()
  }
}

function handleClearInput() {
  if (isPolling.value) return
  inputText.value = ''
  inputValidationError.value = ''
  globalErrorMessage.value = ''
  clientRedFlag.value = null
}

function openHistoryDrawer() {
  if (!isLoggedIn.value) {
    openAuthModal('login')
    return
  }
  showHistoryDrawer.value = true
}

function handleSelectCloudTask(detail: any) {
  stopPolling()
  isPolling.value = false
  inputText.value = detail.originalText || ''
  currentTask.value = {
    taskId: detail.taskId,
    status: detail.status || 'COMPLETED',
    statusDescription: '核验已完成',
    stage: 'FINISHED',
    stageDescription: '核验已完成',
    progressPercent: 100,
    articleSummary: detail.summary,
    claims: detail.claims || [],
    totalClaimCount: detail.claimCount || (detail.claims ? detail.claims.length : 0),
    completedClaimCount: detail.claimCount || (detail.claims ? detail.claims.length : 0),
    unverifiedItems: [],
    redFlag: null,
    allowClientCache: true,
    updatedAt: detail.createdAt
  } as unknown as FactCheckTaskResponse
  clientRedFlag.value = null
  globalErrorMessage.value = ''
  isLocalCacheHit.value = true
  showHistoryDrawer.value = false
}

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.page-wrapper {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0 16px 14px;
}

.workspace-container {
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: stretch;
  gap: 20px;
}

.result-panel-card {
  flex: 1;
  min-width: 0;
  background: var(--card-bg);
  border-radius: 20px;
  padding: 18px 22px 14px;
  box-shadow: var(--card-shadow);
  border: 1px solid rgba(226, 232, 240, 0.85);
  display: flex;
  flex-direction: column;
}

.result-scroll-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
  margin-top: 4px;
}

.notice-alert-box {
  margin-top: 12px;
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 12px;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.notice-svg-icon {
  color: #0284c7;
  flex-shrink: 0;
}

.notice-text {
  font-size: 14px;
  font-weight: 600;
  color: #0369a1;
  line-height: 1.5;
}

.claims-list-container {
  display: flex;
  flex-direction: column;
}

.empty-guide-state {
  padding: 36px 20px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.empty-illustration-box {
  width: 60px;
  height: 60px;
  border-radius: 16px;
  background: #eff6ff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-title {
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
}

.empty-desc {
  max-width: 520px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
}

.stamp-legend-row {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 6px;
}

.legend-chip {
  font-size: 12px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 6px;
}

.chip-supported {
  background: #ecfdf5;
  color: #059669;
  border: 1px solid #a7f3d0;
}

.chip-misleading {
  background: #fff1f2;
  color: #e11d48;
  border: 1px solid #fecdd3;
}

.chip-conditional {
  background: #fffbeb;
  color: #d97706;
  border: 1px solid #fde68a;
}

.chip-unverified {
  background: #f8fafc;
  color: #64748b;
  border: 1px solid #cbd5e1;
}

.chip-doctor {
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
}

.result-footer-disclaimer {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #e2e8f0;
  font-size: 11px;
  color: #94a3b8;
  text-align: center;
  flex-shrink: 0;
}

/* 当页面宽度不足时（<= 1024px），仅显示左侧内容，右侧内容不显示 */
@media (max-width: 1024px) {
  .page-wrapper {
    height: auto;
    min-height: 100vh;
    overflow: visible;
  }

  .workspace-container {
    flex-direction: column;
  }

  .result-panel-card {
    display: none !important;
  }

  .workspace-container > :deep(.input-panel-card),
  .workspace-container > * {
    max-width: 100% !important;
    width: 100% !important;
  }
}

@media (max-height: 700px) and (min-width: 1025px) {
  .page-wrapper {
    height: auto;
    min-height: 100vh;
    overflow: visible;
  }

  .result-scroll-content {
    overflow-y: visible;
  }
}
</style>
