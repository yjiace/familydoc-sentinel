<template>
  <!-- 危急红线置顶警报与提示组件：纯 SVG 矢量图标 -->
  <div v-if="redFlag && redFlag.level !== 'none'" class="redflag-container">
    <!-- 拦截级（HARD） -->
    <div v-if="redFlag.level === 'hard'" class="redflag-hard" role="alert" aria-live="assertive">
      <div class="hard-header">
        <span class="emergency-badge">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
            <line x1="12" y1="9" x2="12" y2="13" />
            <line x1="12" y1="17" x2="12.01" y2="17" />
          </svg>
          <span>危急重症急救警报</span>
        </span>
      </div>
      <p class="hard-message">
        {{ redFlag.message || defaultHardMessage }}
      </p>
      <div class="hard-actions">
        <a href="tel:120" class="btn-call-120">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z" />
          </svg>
          <span>一键拨打 120 急救电话</span>
        </a>
        <button
          type="button"
          class="link-continue-verify"
          @click="$emit('continueAsArticle')"
        >
          这只是文章里的内容（继续核验）
        </button>
      </div>
    </div>

    <!-- 提示级（WARN） -->
    <div v-else-if="redFlag.level === 'warn'" class="redflag-warn" role="status">
      <div class="warn-content">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#d97706" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z" />
          <line x1="12" y1="9" x2="12" y2="13" />
          <line x1="12" y1="17" x2="12.01" y2="17" />
        </svg>
        <p class="warn-message">
          {{ redFlag.message || defaultWarnMessage }}
        </p>
      </div>
      <a href="tel:120" class="warn-call-link">拨打 120</a>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { RedFlagResult } from '../types/factcheck'
import { HARD_RED_FLAG_MESSAGE, WARN_RED_FLAG_MESSAGE } from '../utils/clientRedFlagScanner'

defineProps<{
  redFlag?: RedFlagResult | null
}>()

defineEmits<{
  (e: 'continueAsArticle'): void
}>()

const defaultHardMessage = HARD_RED_FLAG_MESSAGE
const defaultWarnMessage = WARN_RED_FLAG_MESSAGE
</script>

<style scoped>
.redflag-container {
  margin-bottom: 12px;
}

.redflag-hard {
  background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
  border: 2px solid #dc2626;
  border-radius: 14px;
  padding: 16px 18px;
  box-shadow: 0 8px 20px -4px rgba(220, 38, 38, 0.2);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.hard-header {
  display: flex;
  align-items: center;
}

.emergency-badge {
  background: #dc2626;
  color: #ffffff;
  font-size: 13px;
  font-weight: 800;
  padding: 3px 10px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.hard-message {
  font-size: 15px;
  font-weight: 800;
  color: #991b1b;
  line-height: 1.5;
}

.hard-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.btn-call-120 {
  background: #dc2626;
  color: #ffffff;
  font-size: 14px;
  font-weight: 800;
  padding: 7px 16px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  text-decoration: none;
  box-shadow: 0 4px 10px rgba(220, 38, 38, 0.3);
}

.link-continue-verify {
  font-size: 13px;
  font-weight: 600;
  color: #991b1b;
  text-decoration: underline;
  padding: 4px;
}

.redflag-warn {
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 12px;
  padding: 10px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.warn-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.warn-message {
  font-size: 13px;
  font-weight: 700;
  color: #92400e;
}

.warn-call-link {
  font-size: 13px;
  font-weight: 700;
  color: #dc2626;
  text-decoration: underline;
  white-space: nowrap;
}
</style>
