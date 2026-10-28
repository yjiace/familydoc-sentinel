<template>
  <!-- 核验结论印章组件：1:1 对齐原型图右上角倾斜仿真印章视觉，纯矢量五角星与高质感边框 -->
  <div
    v-if="stamp && verdictCode !== 'RETRY_NEEDED'"
    class="verdict-stamp-wrapper"
    :aria-label="`核验结论：${stamp.colloquialText}（${stamp.rigorousText}）`"
  >
    <div
      class="stamp-seal stamp-animate"
      :class="stampShapeClass"
      :style="stampColorStyle"
    >
      <div class="stamp-inner-ring">
        <span class="rigorous-small">{{ stamp.rigorousText }}</span>
        <!-- 纯矢量精致五角星 SVG -->
        <svg class="stamp-star-svg" viewBox="0 0 24 24" width="10" height="10" fill="currentColor" aria-hidden="true">
          <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
        </svg>
        <span class="colloquial-large">{{ stamp.colloquialText }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { StampDto } from '../types/factcheck'

const props = defineProps<{
  stamp?: StampDto | null
  verdictCode?: string
}>()

const stampShapeClass = computed(() => {
  const code = props.verdictCode || props.stamp?.verdictCode || ''
  switch (code) {
    case 'SUPPORTED':
      return 'shape-supported'
    case 'MISLEADING':
      return 'shape-misleading'
    case 'CONDITIONAL':
      return 'shape-conditional'
    case 'ASK_DOCTOR':
      return 'shape-ask-doctor'
    case 'UNVERIFIED':
    default:
      return 'shape-unverified'
  }
})

const stampColorStyle = computed(() => {
  const code = props.verdictCode || props.stamp?.verdictCode || ''
  // 附件图没查实印章为高质感印泥红粉色
  let hex = props.stamp?.colorHex || '#ef4444'
  if (code === 'UNVERIFIED') {
    hex = '#f43f5e'
  }
  return {
    '--seal-color': hex
  }
})
</script>

<style scoped>
.verdict-stamp-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
  flex-shrink: 0;
}

.stamp-seal {
  position: relative;
  color: var(--seal-color, #ef4444);
  display: flex;
  align-items: center;
  justify-content: center;
  transform: rotate(-15deg);
}

/* 附件同款圆形双环印章（UNVERIFIED） */
.shape-unverified {
  width: 84px;
  height: 84px;
  border-radius: 50%;
  border: 2px solid var(--seal-color);
  background: rgba(244, 63, 94, 0.04);
  padding: 2.5px;
}

.shape-unverified .stamp-inner-ring {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 1px solid var(--seal-color);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1px;
}

/* 其他结论印章外框 */
.shape-supported,
.shape-misleading,
.shape-conditional,
.shape-ask-doctor {
  width: 82px;
  height: 82px;
  border-radius: 16px;
  border: 2px solid var(--seal-color);
  padding: 3px;
}

.shape-supported .stamp-inner-ring,
.shape-misleading .stamp-inner-ring,
.shape-conditional .stamp-inner-ring,
.shape-ask-doctor .stamp-inner-ring {
  width: 100%;
  height: 100%;
  border-radius: 12px;
  border: 1px solid var(--seal-color);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1px;
}

.rigorous-small {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.8px;
  opacity: 0.9;
  line-height: 1;
}

.stamp-star-svg {
  margin: 1px 0;
  opacity: 0.85;
}

.colloquial-large {
  font-size: 16px;
  font-weight: 900;
  letter-spacing: 1.5px;
  line-height: 1.1;
  font-family: 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}
</style>
