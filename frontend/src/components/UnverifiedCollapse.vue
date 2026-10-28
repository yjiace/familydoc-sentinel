<template>
  <!-- 未纳入核验的内容折叠清单组件：纯 SVG 矢量图标 -->
  <div v-if="items && items.length > 0" class="unverified-collapse">
    <button
      type="button"
      class="collapse-trigger"
      @click="isOpen = !isOpen"
      :aria-expanded="isOpen"
    >
      <div class="trigger-left">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#2563eb" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2" />
          <rect x="8" y="2" width="8" height="4" rx="1" ry="1" />
        </svg>
        <span>文中另有 <strong>{{ items.length }}</strong> 条内容未纳入核验（点击查看原因）</span>
      </div>
      <svg class="collapse-arrow-svg" :class="{ 'is-open': isOpen }" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <polyline points="9 18 15 12 9 6" />
      </svg>
    </button>

    <div v-if="isOpen" class="collapse-body">
      <div
        v-for="(item, idx) in items"
        :key="idx"
        class="unverified-item"
      >
        <div class="segment-text">“{{ item.segmentText }}”</div>
        <div class="reason-tag">未核验原因：{{ item.reasonDescription }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { UnverifiedItemDto } from '../types/factcheck'

defineProps<{
  items?: UnverifiedItemDto[]
}>()

const isOpen = ref(false)
</script>

<style scoped>
.unverified-collapse {
  margin-top: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #f8fafc;
  overflow: hidden;
}

.collapse-trigger {
  width: 100%;
  padding: 10px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  transition: background-color 0.15s;
}

.collapse-trigger:hover {
  background: #f1f5f9;
}

.trigger-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.trigger-left strong {
  color: #0f172a;
}

.collapse-arrow-svg {
  color: #94a3b8;
  transition: transform 0.2s ease;
}

.collapse-arrow-svg.is-open {
  transform: rotate(90deg);
}

.collapse-body {
  padding: 10px 14px 12px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: #ffffff;
}

.unverified-item {
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.segment-text {
  font-size: 13px;
  color: #1e293b;
  font-weight: 600;
}

.reason-tag {
  font-size: 12px;
  color: #64748b;
}
</style>
