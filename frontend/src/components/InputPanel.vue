<template>
  <!-- 左栏输入与示例面板：对齐原型图布局，采用高保真矢量 SVG，支持一屏紧凑布局 -->
  <aside class="input-panel-wrap">
    <div class="input-card">
      <!-- 顶部标题与 2000 字计数 -->
      <div class="panel-header">
        <div class="header-title">
          <!-- 蓝紫渐变四角星 SVG 图标 -->
          <svg class="sparkle-svg" viewBox="0 0 24 24" width="20" height="20" fill="none" aria-hidden="true">
            <path
              d="M12 2L14.4 9.6L22 12L14.4 14.4L12 22L9.6 14.4L2 12L9.6 9.6L12 2Z"
              fill="url(#sparkleGrad)"
            />
            <defs>
              <linearGradient id="sparkleGrad" x1="2" y1="2" x2="22" y2="22" gradientUnits="userSpaceOnUse">
                <stop stop-color="#3b82f6" />
                <stop offset="1" stop-color="#6366f1" />
              </linearGradient>
            </defs>
          </svg>
          <h2>输入待核验文章或说法</h2>
        </div>
        <div class="char-counter" :class="{ 'is-over': charCount > 2000 }">
          <strong class="current-count">{{ charCount }}</strong>
          <span> / 2000 字</span>
        </div>
      </div>

      <!-- 文本输入框 -->
      <div class="textarea-box" :class="{ 'has-error': isOverLimit || !!validationError }">
        <textarea
          :value="modelValue"
          @input="onInput"
          class="rumor-textarea"
          rows="4"
          placeholder="请在此粘贴健康文章、养生流言或用药说法（最多 2000 字）..."
          :disabled="loading"
          aria-label="输入待核验的健康文章或说法"
        ></textarea>
      </div>

      <!-- 字数超限或为空错误提示 -->
      <div v-if="isOverLimit || validationError" class="input-error-tip" role="alert">
        {{ isOverLimit ? '请输入 2000 字以内的文字' : validationError }}
      </div>

      <!-- 试试这些例子与换一批 -->
      <div class="examples-section">
        <div class="examples-header">
          <span class="examples-label">试试这些例子：</span>
          <button
            type="button"
            class="refresh-btn"
            @click="rotateExamples"
            :disabled="loading"
            title="换一批示例"
          >
            <!-- 旋转循环双箭头 SVG -->
            <svg class="refresh-svg" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M21 2v6h-6M3 12a9 9 0 0 1 15-6.7L21 8M3 22v-6h6M21 12a9 9 0 0 1-15 6.7L3 16" />
            </svg>
            <span>换一批</span>
          </button>
        </div>

        <ul class="examples-list">
          <li
            v-for="item in currentExamples"
            :key="item.text"
            class="example-item"
            @click="selectExample(item.text)"
          >
            <div class="example-left">
              <!-- 分类微图标：绿叶(lifestyle) / 心电(disease) / 胶囊(drug) -->
              <span class="example-icon-badge" :class="`type-${item.type}`" aria-hidden="true">
                <!-- 绿叶 -->
                <svg v-if="item.type === 'lifestyle'" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10Z" />
                  <path d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12" />
                </svg>
                <!-- 心电图脉冲 -->
                <svg v-else-if="item.type === 'disease'" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
                </svg>
                <!-- 药丸/链接胶囊 -->
                <svg v-else viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="m10.5 20.5 10-10a4.95 4.95 0 1 0-7-7l-10 10a4.95 4.95 0 1 0 7 7Z" />
                  <path d="m8.5 8.5 7 7" />
                </svg>
              </span>
              <span class="example-text">{{ item.text }}</span>
            </div>
            <!-- 右侧浅灰箭头 ChevronRight -->
            <svg class="example-arrow-svg" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M9 18l6-6-6-6" />
            </svg>
          </li>
        </ul>
      </div>

      <!-- 操作按钮区 -->
      <div class="action-buttons">
        <button
          v-if="!loading"
          type="button"
          class="btn-primary"
          @click="$emit('submit')"
        >
          <!-- 放大镜 SVG -->
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <span>开始核验</span>
        </button>

        <!-- 核验中支持取消 -->
        <button
          v-else
          type="button"
          class="btn-cancel"
          @click="$emit('cancel')"
        >
          <svg class="cancel-spin-svg" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <line x1="12" y1="2" x2="12" y2="6" />
            <line x1="12" y1="18" x2="12" y2="22" />
            <line x1="4.93" y1="4.93" x2="7.76" y2="7.76" />
            <line x1="16.24" y1="16.24" x2="19.07" y2="19.07" />
            <line x1="2" y1="12" x2="6" y2="12" />
            <line x1="18" y1="12" x2="22" y2="12" />
            <line x1="4.93" y1="19.07" x2="7.76" y2="16.24" />
            <line x1="16.24" y1="7.76" x2="19.07" y2="4.93" />
          </svg>
          <span>核验中 · 点击取消</span>
        </button>

        <button
          type="button"
          class="btn-clear"
          @click="$emit('clear')"
          :disabled="loading || !modelValue"
        >
          <!-- 垃圾桶 SVG -->
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <polyline points="3 6 5 6 21 6" />
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
          </svg>
          <span>清空内容</span>
        </button>
      </div>

      <!-- 用户双链额度（每日/每周/每月）常驻监控区 -->
<!--      <div v-if="isLoggedIn && userUsage" class="quota-overview-box" :class="{ 'quota-box-exceeded': userUsage.quotaExceeded }">-->
<!--        <div class="quota-box-header">-->
<!--          <span class="quota-box-title">双链额度用量（满额停止服务）</span>-->
<!--          <span class="quota-box-badge" :class="userUsage.quotaExceeded ? 'badge-stop' : 'badge-active'">-->
<!--            {{ userUsage.quotaExceeded ? '额度已满 · 停止服务' : '额度正常' }}-->
<!--          </span>-->
<!--        </div>-->

<!--        <div v-if="userUsage.quotaExceeded && userUsage.quotaExceededReason" class="quota-stop-reason">-->
<!--          {{ userUsage.quotaExceededReason }}-->
<!--        </div>-->

<!--        <div class="quota-grid-table">-->
<!--          &lt;!&ndash; 每日 &ndash;&gt;-->
<!--          <div class="quota-col-item">-->
<!--            <div class="quota-col-head">每日额度</div>-->
<!--            <div class="quota-metric-line">-->
<!--              <span class="metric-name">Token</span>-->
<!--              <span class="metric-val" :class="{ 'val-over': isOver(userUsage.dailyTokensUsed, userUsage.dailyTokenLimit) }">-->
<!--                {{ formatTokenNum(userUsage.dailyTokensUsed) }}/{{ formatTokenNum(userUsage.dailyTokenLimit || 50000) }}-->
<!--              </span>-->
<!--            </div>-->
<!--            <div class="mini-bar">-->
<!--              <div-->
<!--                class="mini-bar-fill fill-blue"-->
<!--                :class="barStateClass(userUsage.dailyTokensUsed, userUsage.dailyTokenLimit)"-->
<!--                :style="{ width: calcPct(userUsage.dailyTokensUsed, userUsage.dailyTokenLimit) + '%' }"-->
<!--              ></div>-->
<!--            </div>-->
<!--            <div class="quota-metric-line">-->
<!--              <span class="metric-name">搜索</span>-->
<!--              <span class="metric-val" :class="{ 'val-over': isOver(userUsage.dailySearchUsed, userUsage.dailySearchLimit) }">-->
<!--                {{ userUsage.dailySearchUsed || 0 }}/{{ userUsage.dailySearchLimit || 30 }}次-->
<!--              </span>-->
<!--            </div>-->
<!--            <div class="mini-bar">-->
<!--              <div-->
<!--                class="mini-bar-fill fill-cyan"-->
<!--                :class="barStateClass(userUsage.dailySearchUsed, userUsage.dailySearchLimit)"-->
<!--                :style="{ width: calcPct(userUsage.dailySearchUsed, userUsage.dailySearchLimit) + '%' }"-->
<!--              ></div>-->
<!--            </div>-->
<!--          </div>-->

<!--          &lt;!&ndash; 每周 &ndash;&gt;-->
<!--          <div class="quota-col-item">-->
<!--            <div class="quota-col-head">每周额度</div>-->
<!--            <div class="quota-metric-line">-->
<!--              <span class="metric-name">Token</span>-->
<!--              <span class="metric-val" :class="{ 'val-over': isOver(userUsage.weeklyTokensUsed, userUsage.weeklyTokenLimit) }">-->
<!--                {{ formatTokenNum(userUsage.weeklyTokensUsed) }}/{{ formatTokenNum(userUsage.weeklyTokenLimit || 200000) }}-->
<!--              </span>-->
<!--            </div>-->
<!--            <div class="mini-bar">-->
<!--              <div-->
<!--                class="mini-bar-fill fill-blue"-->
<!--                :class="barStateClass(userUsage.weeklyTokensUsed, userUsage.weeklyTokenLimit)"-->
<!--                :style="{ width: calcPct(userUsage.weeklyTokensUsed, userUsage.weeklyTokenLimit) + '%' }"-->
<!--              ></div>-->
<!--            </div>-->
<!--            <div class="quota-metric-line">-->
<!--              <span class="metric-name">搜索</span>-->
<!--              <span class="metric-val" :class="{ 'val-over': isOver(userUsage.weeklySearchUsed, userUsage.weeklySearchLimit) }">-->
<!--                {{ userUsage.weeklySearchUsed || 0 }}/{{ userUsage.weeklySearchLimit || 120 }}次-->
<!--              </span>-->
<!--            </div>-->
<!--            <div class="mini-bar">-->
<!--              <div-->
<!--                class="mini-bar-fill fill-cyan"-->
<!--                :class="barStateClass(userUsage.weeklySearchUsed, userUsage.weeklySearchLimit)"-->
<!--                :style="{ width: calcPct(userUsage.weeklySearchUsed, userUsage.weeklySearchLimit) + '%' }"-->
<!--              ></div>-->
<!--            </div>-->
<!--          </div>-->

<!--          &lt;!&ndash; 每月 &ndash;&gt;-->
<!--          <div class="quota-col-item">-->
<!--            <div class="quota-col-head">每月额度</div>-->
<!--            <div class="quota-metric-line">-->
<!--              <span class="metric-name">Token</span>-->
<!--              <span class="metric-val" :class="{ 'val-over': isOver(userUsage.monthlyTokensUsed, userUsage.monthlyTokenLimit) }">-->
<!--                {{ formatTokenNum(userUsage.monthlyTokensUsed) }}/{{ formatTokenNum(userUsage.monthlyTokenLimit || 500000) }}-->
<!--              </span>-->
<!--            </div>-->
<!--            <div class="mini-bar">-->
<!--              <div-->
<!--                class="mini-bar-fill fill-blue"-->
<!--                :class="barStateClass(userUsage.monthlyTokensUsed, userUsage.monthlyTokenLimit)"-->
<!--                :style="{ width: calcPct(userUsage.monthlyTokensUsed, userUsage.monthlyTokenLimit) + '%' }"-->
<!--              ></div>-->
<!--            </div>-->
<!--            <div class="quota-metric-line">-->
<!--              <span class="metric-name">搜索</span>-->
<!--              <span class="metric-val" :class="{ 'val-over': isOver(userUsage.monthlySearchUsed, userUsage.monthlySearchLimit) }">-->
<!--                {{ userUsage.monthlySearchUsed || 0 }}/{{ userUsage.monthlySearchLimit || 300 }}次-->
<!--              </span>-->
<!--            </div>-->
<!--            <div class="mini-bar">-->
<!--              <div-->
<!--                class="mini-bar-fill fill-cyan"-->
<!--                :class="barStateClass(userUsage.monthlySearchUsed, userUsage.monthlySearchLimit)"-->
<!--                :style="{ width: calcPct(userUsage.monthlySearchUsed, userUsage.monthlySearchLimit) + '%' }"-->
<!--              ></div>-->
<!--            </div>-->
<!--          </div>-->
<!--        </div>-->
<!--      </div>-->
    </div>

    <!-- 左栏底部说明 -->
    <div class="panel-footnotes">
      <p>· 比对信息仅供健康参考，不可作为处方或诊疗依据</p>
      <p>· 内容由 AI 辅助生成，非官方结论 · 本地隐私保护</p>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { isLoggedIn, userUsage } from '../utils/userState'

const props = defineProps<{
  modelValue: string
  loading: boolean
  validationError?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'submit'): void
  (e: 'cancel'): void
  (e: 'clear'): void
}>()

const sampleGroups = [
  [
    { type: 'lifestyle', text: '吃黑木耳软化血管降低血压吗？' },
    { type: 'disease', text: '吃大蒜能预防感冒吗？' },
    { type: 'drug', text: '他汀类药物伤肝必须停药？' }
  ],
  [
    { type: 'lifestyle', text: '喝普洱茶能抗癌降脂吗？' },
    { type: 'drug', text: '降压药一旦吃了就一辈子不能停？' },
    { type: 'disease', text: '经常泡脚能彻底治愈糖尿病吗？' }
  ],
  [
    { type: 'lifestyle', text: '每天喝一杯红酒对心脏血管有好处吗？' },
    { type: 'drug', text: '阿司匹林每晚吃一片能防猝死吗？' },
    { type: 'disease', text: '吃碱性食物能改变人体酸碱体质吗？' }
  ]
]

const currentGroupIndex = ref(0)
const currentExamples = computed(() => sampleGroups[currentGroupIndex.value])

const charCount = computed(() => props.modelValue?.length || 0)
const isOverLimit = computed(() => charCount.value > 2000)

function onInput(event: Event) {
  const target = event.target as HTMLTextAreaElement
  emit('update:modelValue', target.value)
}

function rotateExamples() {
  currentGroupIndex.value = (currentGroupIndex.value + 1) % sampleGroups.length
}

function selectExample(text: string) {
  if (props.loading) return
  emit('update:modelValue', text)
}

function formatTokenNum(num?: number): string {
  const n = num || 0
  if (n >= 1000) {
    return (n / 1000).toFixed(1).replace(/\.0$/, '') + 'k'
  }
  return n.toString()
}

function isOver(used?: number, limit?: number): boolean {
  if (!limit || limit <= 0) return false
  return (used || 0) >= limit
}

function calcPct(used?: number, limit?: number): number {
  if (!limit || limit <= 0) return 0
  return Math.max(0, Math.min(100, Math.round(((used || 0) / limit) * 100)))
}

function barStateClass(used?: number, limit?: number): string {
  const pct = calcPct(used, limit)
  if (pct >= 100) return 'mini-fill-danger'
  if (pct >= 80) return 'mini-fill-warn'
  return ''
}
</script>

<style scoped>
.input-panel-wrap {
  width: 380px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 10px;
}

.input-card {
  background: var(--card-bg);
  border-radius: 20px;
  padding: 18px 20px 18px;
  box-shadow: var(--card-shadow);
  border: 1px solid rgba(226, 232, 240, 0.85);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 6px;
}

.sparkle-svg {
  flex-shrink: 0;
}

.header-title h2 {
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
}

.char-counter {
  font-size: 13px;
  color: #94a3b8;
  font-variant-numeric: tabular-nums;
}

.char-counter.is-over {
  color: #dc2626;
  font-weight: 700;
}

.textarea-box {
  background: #f8fafc;
  border: 1.5px solid #e2e8f0;
  border-radius: 14px;
  padding: 10px 14px;
  transition: all 0.2s ease;
}

.textarea-box:focus-within {
  background: #ffffff;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
}

.textarea-box.has-error {
  border-color: #ef4444;
  background: #fef2f2;
}

.rumor-textarea {
  width: 100%;
  border: none;
  background: transparent;
  outline: none;
  resize: none;
  font-size: 14px;
  line-height: 1.6;
  color: #1e293b;
  min-height: 88px;
}

.rumor-textarea::placeholder {
  color: #94a3b8;
}

.input-error-tip {
  font-size: 13px;
  color: #dc2626;
  font-weight: 600;
  margin-top: -6px;
}

.examples-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.examples-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.examples-label {
  font-size: 13px;
  font-weight: 700;
  color: #475569;
}

.refresh-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #2563eb;
  padding: 2px 6px;
  border-radius: 6px;
  transition: opacity 0.15s;
}

.refresh-btn:hover {
  opacity: 0.8;
}

.refresh-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.examples-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.example-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.example-item:hover {
  background: #eff6ff;
  border-color: #bfdbfe;
  transform: translateY(-1px);
}

.example-left {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}

.example-icon-badge {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.type-lifestyle {
  background: #dcfce7;
  color: #16a34a;
}

.type-disease {
  background: #eff6ff;
  color: #2563eb;
}

.type-drug {
  background: #ede9fe;
  color: #7c3aed;
}

.example-text {
  font-size: 13px;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.example-arrow-svg {
  color: #94a3b8;
  flex-shrink: 0;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 4px;
}

.btn-primary {
  width: 100%;
  height: 44px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: #ffffff;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.3);
  transition: all 0.2s ease;
}

.btn-primary:hover {
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.4);
  transform: translateY(-1px);
}

.btn-cancel {
  width: 100%;
  height: 44px;
  background: #fee2e2;
  color: #dc2626;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.cancel-spin-svg {
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

.btn-clear {
  width: 100%;
  height: 38px;
  background: #f1f5f9;
  color: #475569;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.15s;
}

.btn-clear:hover:not(:disabled) {
  background: #e2e8f0;
  color: #0f172a;
}

.btn-clear:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.panel-footnotes {
  font-size: 11px;
  color: #94a3b8;
  line-height: 1.5;
  padding: 0 4px;
}

.quota-overview-box {
  margin-top: 2px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quota-overview-box.quota-box-exceeded {
  background: #fff5f5;
  border-color: #fecaca;
}

.quota-box-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.quota-box-title {
  font-size: 12px;
  font-weight: 700;
  color: #334155;
}

.quota-box-badge {
  font-size: 10.5px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 999px;
}

.badge-active {
  background: #eff6ff;
  color: #2563eb;
}

.badge-stop {
  background: #fee2e2;
  color: #dc2626;
}

.quota-stop-reason {
  font-size: 11.5px;
  font-weight: 600;
  color: #b91c1c;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 6px 8px;
  line-height: 1.4;
}

.quota-grid-table {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
}

.quota-col-item {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 6px 7px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.quota-col-head {
  font-size: 11px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 2px;
}

.quota-metric-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 10px;
  color: #64748b;
}

.metric-val {
  font-weight: 600;
  color: #334155;
  font-variant-numeric: tabular-nums;
}

.val-over {
  color: #dc2626;
  font-weight: 700;
}

.mini-bar {
  width: 100%;
  height: 4px;
  background: #e2e8f0;
  border-radius: 999px;
  overflow: hidden;
  margin-bottom: 2px;
}

.mini-bar-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.25s ease;
}

.fill-blue {
  background: #3b82f6;
}

.fill-cyan {
  background: #0ea5e9;
}

.mini-fill-warn {
  background: #f59e0b !important;
}

.mini-fill-danger {
  background: #ef4444 !important;
}

@media (max-width: 960px) {
  .input-panel-wrap {
    width: 100%;
  }
}
</style>
