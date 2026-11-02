<template>
  <!-- 顶部“整篇核验摘要”栏：1:1 对齐原型图右栏头部，右上角固定“历史记录”胶囊入口 -->
  <div class="summary-bar">
    <div class="summary-left">
      <!-- 蓝色折角文档 SVG 图标 -->
      <div class="doc-icon-box" aria-hidden="true">
        <svg viewBox="0 0 36 36" width="34" height="34" fill="none">
          <rect x="4" y="3" width="28" height="30" rx="7" fill="url(#summaryDocGrad)" />
          <path d="M11 11H25M11 17H25M11 23H19" stroke="#FFFFFF" stroke-width="2.4" stroke-linecap="round" />
          <defs>
            <linearGradient id="summaryDocGrad" x1="4" y1="3" x2="32" y2="33" gradientUnits="userSpaceOnUse">
              <stop stop-color="#3b82f6" />
              <stop offset="1" stop-color="#1d4ed8" />
            </linearGradient>
          </defs>
        </svg>
      </div>

      <div class="summary-texts">
        <div class="summary-title-row">
          <h2 class="summary-heading">整篇核验摘要</h2>
          <!-- 命中缓存徽标与重新核验 -->
          <div v-if="isCacheHit" class="cache-tag-group">
            <span class="cache-badge">本机缓存结果</span>
            <button
              type="button"
              class="reverify-link"
              @click="$emit('reverify')"
              :disabled="loading"
            >
              <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 2v6h-6M3 12a9 9 0 0 1 15-6.7L21 8M3 22v-6h6M21 12a9 9 0 0 1-15 6.7L3 16" />
              </svg>
              <span>重新核验</span>
            </button>
          </div>
        </div>

        <p class="summary-content" v-html="formattedSummary"></p>
      </div>
    </div>

    <!-- 右侧操作区：辟谣长图 + 历史记录 -->
    <div class="summary-actions-right">
      <button
        v-if="canGeneratePoster"
        type="button"
        class="poster-btn"
        @click="$emit('openPoster')"
        title="汇总生成包含本次核验所有说法的辟谣海报长图"
      >
        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <rect width="18" height="18" x="3" y="3" rx="2" ry="2"/>
          <circle cx="9" cy="9" r="2"/>
          <path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"/>
        </svg>
        <span>生成辟谣长图</span>
      </button>

      <!-- 右上角“历史记录”胶囊按钮 -->
      <button
        type="button"
        class="history-btn"
        @click="$emit('openHistory')"
        title="查看本机 24 小时核验缓存记录"
      >
        <!-- 时钟 SVG 图标 -->
        <svg class="history-clock-svg" viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="12" cy="12" r="10" />
          <polyline points="12 6 12 12 16 14" />
        </svg>
        <span>历史记录</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  summaryText?: string
  isCacheHit?: boolean
  loading?: boolean
  canGeneratePoster?: boolean
}>()

defineEmits<{
  (e: 'reverify'): void
  (e: 'openHistory'): void
  (e: 'openPoster'): void
}>()

const formattedSummary = computed(() => {
  const raw = props.summaryText || '请在左侧输入健康说法，点击“开始核验”获取查证结果。'
  const escaped = raw
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  return escaped.replace(/(\d+)/g, '<strong class="num-highlight">$1</strong>')
})
</script>

<style scoped>
.summary-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f1f5f9;
  flex-shrink: 0;
}

.summary-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.doc-icon-box {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  filter: drop-shadow(0 3px 8px rgba(37, 99, 235, 0.22));
}

.summary-texts {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.summary-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.summary-heading {
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.2px;
}

.cache-tag-group {
  display: flex;
  align-items: center;
  gap: 6px;
}

.cache-badge {
  font-size: 11px;
  font-weight: 700;
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  padding: 1px 6px;
  border-radius: 4px;
}

.reverify-link {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: #64748b;
  padding: 1px 5px;
  border-radius: 4px;
}

.reverify-link:hover {
  color: #2563eb;
  background: #f1f5f9;
}

.summary-content {
  font-size: 13px;
  color: #475569;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.summary-content :deep(.num-highlight) {
  font-weight: 800;
  color: #0f172a;
}

.history-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  padding: 6px 14px;
  border-radius: 999px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
  flex-shrink: 0;
  transition: all 0.15s ease;
}

.history-btn:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #0f172a;
}

.history-clock-svg {
  color: #64748b;
}

.summary-actions-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.poster-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 700;
  padding: 6px 14px;
  border-radius: 999px;
  box-shadow: 0 1px 3px rgba(37, 99, 235, 0.08);
  flex-shrink: 0;
  cursor: pointer;
  transition: all 0.15s ease;
}

.poster-btn:hover {
  background: #dbeafe;
  border-color: #93c5fd;
  color: #1e40af;
}

@media (max-width: 640px) {
  .summary-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  .history-btn {
    align-self: flex-end;
  }
}
</style>
