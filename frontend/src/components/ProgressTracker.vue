<template>
  <!-- 实时检索与核验进度看板组件：纯矢量 SVG -->
  <section class="progress-tracker" aria-live="polite">
    <div class="tracker-header">
      <div class="stage-info">
        <span class="pulse-dot" aria-hidden="true"></span>
        <span class="stage-title">{{ task.stageDescription || '正在检索权威来源并核对证据...' }}</span>
      </div>
      <div class="stage-right">
        <span v-if="task.totalClaimCount > 0" class="claim-counter">
          已核验 <strong>{{ task.completedClaimCount }}</strong> / {{ task.totalClaimCount }} 条说法
        </span>
        <span class="percent-badge">{{ task.progressPercent || 15 }}%</span>
      </div>
    </div>

    <!-- 进度条 -->
    <div class="progress-bar-track">
      <div
        class="progress-bar-fill"
        :style="{ width: `${Math.max(8, task.progressPercent || 15)}%` }"
      ></div>
    </div>

    <!-- 流程步骤指示器 -->
    <div class="pipeline-steps">
      <div
        v-for="step in steps"
        :key="step.key"
        class="step-pill"
        :class="getStepClass(step.order)"
      >
        <span class="step-dot"></span>
        <span>{{ step.label }}</span>
      </div>
    </div>

    <!-- 实时检索中的论断 -->
    <div v-if="pendingClaims.length > 0" class="searching-claims-box">
      <div
        v-for="claim in pendingClaims"
        :key="claim.claimId"
        class="searching-claim-row"
      >
        <div class="claim-row-left">
          <svg class="mini-spinner-svg" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <line x1="12" y1="2" x2="12" y2="6" />
            <line x1="12" y1="18" x2="12" y2="22" />
            <line x1="4.93" y1="4.93" x2="7.76" y2="7.76" />
            <line x1="16.24" y1="16.24" x2="19.07" y2="19.07" />
          </svg>
          <span class="searching-claim-text">“{{ claim.claimText }}”</span>
        </div>
        <div class="claim-row-right">
          <span v-if="claim.neutralQuery" class="query-tag" title="中性检索词">
            <svg viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <span>{{ claim.neutralQuery }}</span>
          </span>
          <span class="substage-badge">{{ claim.verifyStageText || '正在检索核对...' }}</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { FactCheckTaskResponse } from '../types/factcheck'

const props = defineProps<{
  task: FactCheckTaskResponse
}>()

const steps = [
  { key: 'SANITIZING', label: '输入清洗', order: 1 },
  { key: 'RED_FLAG_SCANNING', label: '安全红线扫描', order: 2 },
  { key: 'INTENT_ANALYZING', label: '意图识别', order: 3 },
  { key: 'CLAIM_EXTRACTING', label: '论断拆解', order: 4 },
  { key: 'PARALLEL_VERIFYING', label: '多源检索与核对', order: 5 },
  { key: 'SUMMARY_GENERATING', label: '核验总结', order: 6 }
]

const currentStageOrder = computed(() => {
  const stage = props.task.stage
  if (stage === 'SANITIZING') return 1
  if (stage === 'RED_FLAG_SCANNING' || stage === 'CACHE_PROBING') return 2
  if (stage === 'INTENT_ANALYZING') return 3
  if (stage === 'CLAIM_EXTRACTING') return 4
  if (stage === 'PARALLEL_VERIFYING') return 5
  if (stage === 'SUMMARY_GENERATING') return 6
  return 6
})

const pendingClaims = computed(() =>
  (props.task.claims || []).filter(
    (c) => c.verifyStageCode !== 'COMPLETED' && c.verifyStageCode !== 'RETRY_NEEDED' && !c.verdictCode
  )
)

function getStepClass(order: number) {
  if (order < currentStageOrder.value) return 'is-done'
  if (order === currentStageOrder.value) return 'is-active'
  return 'is-pending'
}
</script>

<style scoped>
.progress-tracker {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 14px 18px;
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tracker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stage-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.2);
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(37, 99, 235, 0.4);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(37, 99, 235, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(37, 99, 235, 0);
  }
}

.stage-title {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
}

.stage-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.claim-counter {
  font-size: 12px;
  color: #64748b;
}

.claim-counter strong {
  color: #0f172a;
}

.percent-badge {
  font-size: 12px;
  font-weight: 800;
  color: #2563eb;
  background: #eff6ff;
  padding: 2px 8px;
  border-radius: 999px;
}

.progress-bar-track {
  width: 100%;
  height: 6px;
  background: #e2e8f0;
  border-radius: 999px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6 0%, #1d4ed8 100%);
  border-radius: 999px;
  transition: width 0.3s ease;
}

.pipeline-steps {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.step-pill {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: #f1f5f9;
  color: #64748b;
}

.step-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
}

.step-pill.is-done {
  background: #ecfdf5;
  color: #059669;
}

.step-pill.is-active {
  background: #eff6ff;
  color: #2563eb;
  font-weight: 700;
}

.searching-claims-box {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 4px;
  border-top: 1px solid #e2e8f0;
  padding-top: 8px;
}

.searching-claim-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.claim-row-left {
  display: flex;
  align-items: center;
  gap: 6px;
}

.mini-spinner-svg {
  animation: spin 1.2s linear infinite;
  color: #2563eb;
  flex-shrink: 0;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.searching-claim-text {
  font-size: 12px;
  color: #334155;
  font-weight: 600;
}

.claim-row-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.query-tag {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  background: #f1f5f9;
  color: #64748b;
  padding: 1px 6px;
  border-radius: 4px;
}

.substage-badge {
  font-size: 11px;
  color: #2563eb;
  font-weight: 600;
}
</style>
