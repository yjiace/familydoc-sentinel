<template>
  <!-- 浏览器本地 24 小时缓存记录抽屉 -->
  <div v-if="visible" class="drawer-backdrop" @click.self="$emit('close')">
    <aside class="drawer-panel" role="dialog" aria-label="本机24小时核验缓存">
      <div class="drawer-header">
        <div>
          <h3>本机缓存记录（24小时内）</h3>
          <p class="drawer-subtitle">仅保存在当前浏览器本地，服务端不留存</p>
        </div>
        <button type="button" class="close-btn" @click="$emit('close')" aria-label="关闭">
          <!-- 矢量关闭图标 -->
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>

      <div class="drawer-body">
        <div v-if="entries.length === 0" class="empty-history">
          暂无本机缓存的核验记录
        </div>

        <ul v-else class="history-list">
          <li
            v-for="entry in entries"
            :key="entry.fingerprint"
            class="history-item"
            @click="$emit('select', entry)"
          >
            <div class="item-text">{{ entry.rawText }}</div>
            <div class="item-summary">{{ entry.response.articleSummary }}</div>
            <div class="item-time">缓存时间：{{ formatTime(entry.savedAt) }}</div>
          </li>
        </ul>
      </div>

      <div v-if="entries.length > 0" class="drawer-footer">
        <button type="button" class="btn-clear-cache" @click="$emit('clearAll')">
          清空本机缓存
        </button>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import type { LocalCacheEntry } from '../types/factcheck'

defineProps<{
  visible: boolean
  entries: LocalCacheEntry[]
}>()

defineEmits<{
  (e: 'close'): void
  (e: 'select', entry: LocalCacheEntry): void
  (e: 'clearAll'): void
}>()

function formatTime(ts: number): string {
  try {
    return new Date(ts).toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return ''
  }
}
</script>

<style scoped>
.drawer-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.35);
  backdrop-filter: blur(2px);
  z-index: 950;
  display: flex;
  justify-content: flex-end;
}

.drawer-panel {
  width: 400px;
  max-width: 90vw;
  height: 100%;
  background: #ffffff;
  box-shadow: -8px 0 25px rgba(15, 23, 42, 0.12);
  display: flex;
  flex-direction: column;
}

.drawer-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.drawer-header h3 {
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
}

.drawer-subtitle {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}

.close-btn {
  color: #94a3b8;
  padding: 4px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  color: #0f172a;
  background: #f1f5f9;
}

.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 14px 16px;
}

.empty-history {
  padding: 40px 16px;
  text-align: center;
  font-size: 13px;
  color: #94a3b8;
}

.history-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.history-item {
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 12px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.history-item:hover {
  background: #eff6ff;
  border-color: #bfdbfe;
}

.item-text {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.4;
  margin-bottom: 4px;
}

.item-summary {
  font-size: 12px;
  color: #475569;
  line-height: 1.4;
  margin-bottom: 6px;
}

.item-time {
  font-size: 11px;
  color: #94a3b8;
}

.drawer-footer {
  padding: 14px 18px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  justify-content: flex-end;
}

.btn-clear-cache {
  font-size: 12px;
  font-weight: 600;
  color: #dc2626;
  background: #fee2e2;
  padding: 6px 14px;
  border-radius: 999px;
}
</style>
