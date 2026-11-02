<template>
  <!-- 单条论断核验卡片：1:1 对齐原型图设计视觉与组件结构 -->
  <article class="claim-card" :class="`verdict-${(claim.verdictCode || 'pending').toLowerCase()}`">
    <div class="card-top-row">
      <div class="card-top-left">
        <!-- 类别标签 -->
        <div class="tags-row">
          <span class="category-pill" :class="categoryClass">
            {{ displayCategoryLabel }}
          </span>
          <span v-if="claim.confidenceLabel" class="confidence-pill">
            置信度：{{ claim.confidenceLabel }}
          </span>
        </div>

        <!-- 论断大字标题 -->
        <h3 class="claim-title">“{{ claim.claimText }}”</h3>
      </div>

      <!-- 右上角仿真印章 -->
      <VerdictStamp
        v-if="claim.stamp && claim.verdictCode !== 'RETRY_NEEDED'"
        :stamp="claim.stamp"
        :verdictCode="claim.verdictCode"
      />
    </div>

    <!-- 正在检索中状态 -->
    <div v-if="!claim.verdictCode" class="claim-searching-state">
      <svg class="searching-spin-svg" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <line x1="12" y1="2" x2="12" y2="6" />
        <line x1="12" y1="18" x2="12" y2="22" />
        <line x1="4.93" y1="4.93" x2="7.76" y2="7.76" />
        <line x1="16.24" y1="16.24" x2="19.07" y2="19.07" />
        <line x1="2" y1="12" x2="6" y2="12" />
        <line x1="18" y1="12" x2="22" y2="12" />
        <line x1="4.93" y1="19.07" x2="7.76" y2="16.24" />
        <line x1="16.24" y1="7.76" x2="19.07" y2="4.93" />
      </svg>
      <span>{{ claim.verifyStageText || '正在检索权威医学来源并核对证据...' }}</span>
    </div>

    <!-- 待重试状态（无印章，支持单独重试） -->
    <div v-else-if="claim.verdictCode === 'RETRY_NEEDED'" class="claim-retry-box">
      <div class="retry-info">
        <div class="retry-title-row">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#d97706" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z" />
            <line x1="12" y1="9" x2="12" y2="13" />
            <line x1="12" y1="17" x2="12.01" y2="17" />
          </svg>
          <span class="retry-title">这条暂时没查成</span>
        </div>
        <p class="retry-desc">{{ claim.explanation || '检索服务暂时超时，未影响其他说法的核验结果。' }}</p>
      </div>
      <button
        type="button"
        class="btn-retry-single"
        @click="$emit('retry', claim.claimId)"
        :disabled="retrying"
      >
        <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 2v6h-6M3 12a9 9 0 0 1 15-6.7L21 8M3 22v-6h6M21 12a9 9 0 0 1-15 6.7L3 16" />
        </svg>
        <span>重新核验本条</span>
      </button>
    </div>

    <!-- 有效结论展示 -->
    <template v-else>
      <!-- 核心结论说明色块（左侧竖条指示） -->
      <div class="explanation-banner" :class="bannerColorClass">
        <p class="explanation-text">
          {{ primaryBannerText }}
        </p>
      </div>

      <!-- 没查实时展示“未查实原因与说明”浅蓝底提示框 -->
      <div v-if="claim.verdictCode === 'UNVERIFIED'" class="unverified-reason-box">
        <div class="reason-header">
          <!-- 问号圆圈 SVG -->
          <svg class="question-icon-svg" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <circle cx="12" cy="12" r="10" />
            <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3" />
            <line x1="12" y1="17" x2="12.01" y2="17" />
          </svg>
          <span class="reason-title">
            未查实原因：{{ claim.unverifiedCauseLabel || '证据分歧或冲突' }}
            <template v-if="claim.unverifiedCauseMessage">
              （{{ claim.unverifiedCauseMessage }}）
            </template>
          </span>
        </div>
        <p class="reason-disclaimer">
          说明：{{ claim.unverifiedDisclaimer || '没查实不代表是真的，也不代表是假的，切勿盲目转发。' }}
        </p>
      </div>

      <!-- 补充说明提示 -->
      <div v-if="claim.extraNote" class="extra-note-box">
        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" />
        </svg>
        <span>补充说明：{{ claim.extraNote }}</span>
      </div>

      <!-- 药物安全固定提示 -->
      <div v-if="claim.drugSafetyNotice || claim.categoryCode === 'DRUG'" class="drug-safety-box">
        <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="#7c3aed" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="m10.5 20.5 10-10a4.95 4.95 0 1 0-7-7l-10 10a4.95 4.95 0 1 0 7 7Z" />
          <path d="m8.5 8.5 7 7" />
        </svg>
        <p class="drug-text">
          {{
            claim.drugSafetyNotice ||
            '药物相关提示：请勿自行停药、换药或加减剂量，如有疑问请咨询医生或药师。'
          }}
        </p>
      </div>

      <!-- 查证依据出处列表 -->
      <EvidenceList
        :evidences="claim.evidences"
        :opposingEvidences="claim.opposingEvidences"
        :unverifiedCauseCode="claim.unverifiedCauseCode"
      />

      <!-- 卡片底部操作栏：生成全量辟谣长图海报 -->
      <div class="card-action-bar">
        <button
          type="button"
          class="btn-poster-action"
          @click="$emit('openPoster')"
          title="生成包含本次核验所有内容的辟谣长图"
        >
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <rect width="18" height="18" x="3" y="3" rx="2" ry="2"/>
            <circle cx="9" cy="9" r="2"/>
            <path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"/>
          </svg>
          <span>生成辟谣海报（发长辈/群聊）</span>
        </button>
      </div>
    </template>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ClaimItemDto } from '../types/factcheck'
import VerdictStamp from './VerdictStamp.vue'
import EvidenceList from './EvidenceList.vue'

const props = defineProps<{
  claim: ClaimItemDto
  retrying?: boolean
}>()

const emit = defineEmits<{
  (e: 'retry', claimId: string): void
  (e: 'openPoster'): void
}>()

const displayCategoryLabel = computed(() => {
  if (props.claim.categoryCode === 'LIFESTYLE') {
    return '饮食与生活方式'
  }
  return props.claim.categoryLabel || '健康说法'
})

const categoryClass = computed(() => {
  switch (props.claim.categoryCode) {
    case 'DRUG':
      return 'cat-drug'
    case 'DISEASE':
      return 'cat-disease'
    case 'LIFESTYLE':
    default:
      return 'cat-lifestyle'
  }
})

const bannerColorClass = computed(() => {
  switch (props.claim.verdictCode) {
    case 'SUPPORTED':
      return 'banner-supported'
    case 'MISLEADING':
      return 'banner-misleading'
    case 'CONDITIONAL':
      return 'banner-conditional'
    case 'ASK_DOCTOR':
      return 'banner-doctor'
    case 'UNVERIFIED':
    default:
      return 'banner-unverified'
  }
})

const primaryBannerText = computed(() => {
  return (
    props.claim.explanation ||
    '检索到的可信医学文献对该说法存在不同观点与依据冲突，未形成统一定论。'
  )
})
</script>

<style scoped>
.claim-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 16px 20px;
  margin-top: 14px;
  border: 1px solid #f1f5f9;
  display: flex;
  flex-direction: column;
  gap: 12px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.03);
}

.card-top-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.card-top-left {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.tags-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.category-pill {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
}

.cat-lifestyle {
  background: #eaf7ed;
  color: #16a34a;
}

.cat-disease {
  background: #eff6ff;
  color: #2563eb;
}

.cat-drug {
  background: #f5f3ff;
  color: #7c3aed;
}

.confidence-pill {
  font-size: 11px;
  color: #64748b;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 4px;
}

.claim-title {
  font-size: 18px;
  font-weight: 800;
  color: #15803d;
  line-height: 1.35;
  letter-spacing: -0.2px;
}

.verdict-misleading .claim-title {
  color: #be123c;
}

.verdict-conditional .claim-title {
  color: #b45309;
}

.verdict-unverified .claim-title {
  color: #15803d; /* 原型图中经常吃黑木耳标题为深绿 */
}

.claim-searching-state {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f8fafc;
  padding: 12px 16px;
  border-radius: 10px;
  font-size: 13px;
  color: #2563eb;
}

.searching-spin-svg {
  animation: spin 1.2s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.explanation-banner {
  border-radius: 8px;
  padding: 10px 14px;
  background: #f0fdf4;
  border-left: 4px solid #16a34a;
}

.banner-supported {
  background: #f0fdf4;
  border-left-color: #16a34a;
}

.banner-misleading {
  background: #fff1f2;
  border-left-color: #e11d48;
}

.banner-conditional {
  background: #fffbeb;
  border-left-color: #d97706;
}

.banner-unverified {
  background: #f0fdf4; /* 原型图中为浅绿底与深绿竖条 */
  border-left: 4px solid #16a34a;
}

.banner-doctor {
  background: #eff6ff;
  border-left-color: #2563eb;
}

.explanation-text {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.5;
}

.unverified-reason-box {
  background: #f0f7ff;
  border: 1px solid #e0f2fe;
  border-radius: 10px;
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.reason-header {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #2563eb;
}

.question-icon-svg {
  flex-shrink: 0;
}

.reason-title {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.reason-disclaimer {
  font-size: 12px;
  color: #64748b;
  line-height: 1.45;
}

.extra-note-box {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 12px;
  color: #475569;
}

.drug-safety-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #faf5ff;
  border: 1px solid #f3e8ff;
  border-radius: 8px;
  padding: 8px 12px;
}

.drug-text {
  font-size: 12px;
  color: #6b21a8;
  font-weight: 600;
}

.claim-retry-box {
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 10px;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.retry-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.retry-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.retry-title {
  font-size: 14px;
  font-weight: 700;
  color: #92400e;
}

.retry-desc {
  font-size: 12px;
  color: #b45309;
}

.btn-retry-single {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: #ffffff;
  border: 1px solid #d97706;
  color: #b45309;
  font-size: 12px;
  font-weight: 700;
  padding: 6px 12px;
  border-radius: 999px;
  flex-shrink: 0;
  transition: all 0.15s;
}

.btn-retry-single:hover:not(:disabled) {
  background: #fef3c7;
}

.card-action-bar {
  display: flex;
  justify-content: flex-end;
  padding-top: 8px;
  border-top: 1px dashed #e2e8f0;
}

.btn-poster-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  color: #334155;
  font-size: 12px;
  font-weight: 600;
  padding: 5px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-poster-action:hover {
  background: #eff6ff;
  border-color: #93c5fd;
  color: #1d4ed8;
}
</style>
