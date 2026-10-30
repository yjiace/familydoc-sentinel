<template>
  <!-- 核验历史记录抽屉（统一使用 PostgreSQL 云端数据库持久化存储） -->
  <div v-if="visible" class="drawer-backdrop" @click.self="$emit('close')">
    <aside class="drawer-panel" role="dialog" aria-label="核验历史记录">
      <div class="drawer-header">
        <div class="header-titles">
          <div class="title-with-badge">
            <h3>核验历史记录</h3>
            <span class="cloud-badge">云端存储 (PostgreSQL)</span>
          </div>
          <p class="drawer-subtitle">
            您的全部核验结果已安全存储于云端数据库
          </p>
        </div>
        <button type="button" class="close-btn" @click="$emit('close')" aria-label="关闭">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>

      <!-- 未登录时的引导登录横幅 -->
      <div v-if="!isLoggedIn" class="guest-login-tip">
        <div class="tip-content">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#2563EB" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="16" x2="12" y2="12" />
            <line x1="12" y1="8" x2="12.01" y2="8" />
          </svg>
          <span>登录账号即可查看和同步所有核验历史</span>
        </div>
        <button type="button" class="tip-login-btn" @click="handleOpenLogin">
          立即登录
        </button>
      </div>

      <div class="drawer-body">
        <!-- 加载中状态 -->
        <div v-if="isLoadingCloud" class="empty-history">
          <div class="loading-spinner"></div>
          <p>正在读取云端历史记录...</p>
        </div>

        <!-- 空状态 -->
        <div v-else-if="displayList.length === 0" class="empty-history">
          <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="#94a3b8" stroke-width="1.8">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            <polyline points="14 2 14 8 20 8" />
            <line x1="16" y1="13" x2="8" y2="13" />
            <line x1="16" y1="17" x2="8" y2="17" />
            <polyline points="10 9 9 9 8 9" />
          </svg>
          <p>暂无核验历史记录</p>
        </div>

        <!-- 列表 -->
        <ul v-else class="history-list">
          <li
            v-for="item in displayList"
            :key="item.taskId"
            class="history-item"
            @click="handleSelect(item)"
          >
            <div class="item-header-row">
              <div class="item-text">{{ item.originalText }}</div>
              <button
                type="button"
                class="delete-item-btn"
                title="删除此条记录"
                @click.stop="handleDeleteItem(item.taskId)"
              >
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="3 6 5 6 21 6" />
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                </svg>
              </button>
            </div>
            <div v-if="item.summary" class="item-summary">{{ item.summary }}</div>
            <div class="item-meta-row">
              <span class="meta-time">{{ formatIsoTime(item.createdAt) }}</span>
              <div class="meta-badges">
                <span v-if="item.claimCount" class="meta-claims-count">{{ item.claimCount }} 条说法</span>
                <span class="meta-usage-badge meta-token-badge">Token: {{ item.tokensUsed || 0 }}</span>
                <span class="meta-usage-badge meta-search-badge">搜索: {{ item.searchUsed || 0 }}次</span>
              </div>
            </div>
          </li>
        </ul>
      </div>

      <div v-if="displayList.length > 0" class="drawer-footer">
        <button type="button" class="btn-clear-cache" @click="handleClearAll">
          清空全部历史记录
        </button>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { HistoryTaskSummary } from '../types/user'
import { isLoggedIn, openAuthModal } from '../utils/userState'
import {
  fetchUserHistoryTasks,
  fetchHistoryTaskDetail,
  deleteHistoryTask,
  clearAllHistoryTasks
} from '../api/historyApi'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'selectCloudTask', detail: any): void
}>()

const cloudTasks = ref<HistoryTaskSummary[]>([])
const isLoadingCloud = ref<boolean>(false)

// 监听抽屉打开与登录状态，自动拉取数据库云端记录
watch(
  () => [props.visible, isLoggedIn.value],
  async ([vis, logged]) => {
    if (vis && logged) {
      await loadCloudHistory()
    } else if (!logged) {
      cloudTasks.value = []
    }
  },
  { immediate: true }
)

async function loadCloudHistory(): Promise<void> {
  isLoadingCloud.value = true
  try {
    const page: any = await fetchUserHistoryTasks(0, 50)
    const list = page?.content || (Array.isArray(page) ? page : [])
    cloudTasks.value = list
  } catch (err) {
    cloudTasks.value = []
  } finally {
    isLoadingCloud.value = false
  }
}

const displayList = computed<HistoryTaskSummary[]>(() => {
  return cloudTasks.value || []
})

async function handleSelect(item: HistoryTaskSummary): Promise<void> {
  try {
    const detail = await fetchHistoryTaskDetail(item.taskId)
    emit('selectCloudTask', detail)
    emit('close')
  } catch {
    // 容错处理
  }
}

async function handleDeleteItem(taskId: string): Promise<void> {
  try {
    await deleteHistoryTask(taskId)
    cloudTasks.value = cloudTasks.value.filter((t) => t.taskId !== taskId)
  } catch {
    // 忽略错误
  }
}

async function handleClearAll(): Promise<void> {
  try {
    await clearAllHistoryTasks()
    cloudTasks.value = []
  } catch {
    // 忽略
  }
}

function handleOpenLogin(): void {
  openAuthModal('login')
}

function formatIsoTime(iso: string): string {
  if (!iso) return ''
  try {
    return new Date(iso).toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return iso || ''
  }
}
</script>

<style scoped>
.drawer-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(4px);
  z-index: 950;
  display: flex;
  justify-content: flex-end;
}

.drawer-panel {
  width: 460px;
  max-width: 92vw;
  height: 100%;
  background: #ffffff;
  box-shadow: -10px 0 30px rgba(15, 23, 42, 0.15);
  display: flex;
  flex-direction: column;
}

.drawer-header {
  padding: 18px 20px 14px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.title-with-badge {
  display: flex;
  align-items: center;
  gap: 8px;
}

.drawer-header h3 {
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.cloud-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 10px;
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
}

.drawer-subtitle {
  font-size: 12px;
  color: #64748b;
  margin: 4px 0 0;
}

.close-btn {
  color: #94a3b8;
  padding: 4px;
  border-radius: 6px;
  background: transparent;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  color: #0f172a;
  background: #f1f5f9;
}

.guest-login-tip {
  margin: 12px 16px 0;
  padding: 10px 12px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.tip-content {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #166534;
}

.tip-login-btn {
  padding: 4px 10px;
  background: #2563eb;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 14px 16px;
}

.empty-history {
  padding: 60px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #94a3b8;
  font-size: 13.5px;
}

.history-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.history-item {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px 14px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.history-item:hover {
  background: #eff6ff;
  border-color: #93c5fd;
  transform: translateY(-1px);
}

.item-header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.item-text {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.delete-item-btn {
  background: transparent;
  border: none;
  color: #94a3b8;
  padding: 3px;
  cursor: pointer;
  border-radius: 4px;
}

.delete-item-btn:hover {
  color: #ef4444;
  background: #fee2e2;
}

.item-summary {
  font-size: 12px;
  color: #475569;
  line-height: 1.4;
  margin: 6px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 11px;
  color: #94a3b8;
  margin-top: 8px;
}

.meta-badges {
  display: flex;
  align-items: center;
  gap: 6px;
}

.meta-claims-count {
  background: #e2e8f0;
  color: #475569;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 4px;
}

.meta-usage-badge {
  font-size: 10.5px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 4px;
}

.meta-token-badge {
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
}

.meta-search-badge {
  background: #f0fdfa;
  color: #0d9488;
  border: 1px solid #99f6e4;
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
  border: none;
  cursor: pointer;
}

.btn-clear-cache:hover {
  background: #fecaca;
}

.loading-spinner {
  width: 22px;
  height: 22px;
  border: 2.5px solid #e2e8f0;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
