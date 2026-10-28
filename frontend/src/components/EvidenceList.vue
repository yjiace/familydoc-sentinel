<template>
  <!-- 查证依据与出处原文列表组件：1:1 对齐原型图视觉，矢量图标与紧凑结构 -->
  <div v-if="hasAnyEvidence" class="evidence-section">
    <h4 class="evidence-heading">查证依据与出处原文：</h4>

    <!-- 来源冲突双栏展示 -->
    <template v-if="isSourceConflict && opposingEvidences && opposingEvidences.length > 0">
      <div class="conflict-columns">
        <div class="conflict-col">
          <div class="conflict-col-title side-a">观点一方出处</div>
          <div class="evidence-list">
            <div
              v-for="item in evidences"
              :key="item.url + item.index"
              class="evidence-card"
              @click="toggleExpand(item.url)"
            >
              <div class="evidence-main">
                <div class="quote-row">
                  <span class="stance-badge" :class="getStanceClass(item)">
                    <component :is="getStanceIcon(item)" class="stance-icon-svg" />
                    <span>{{ getStanceBadgeText(item) }}</span>
                  </span>
                  <span class="quote-text">“{{ item.quote || item.title }}”</span>
                </div>
                <div class="meta-row">
                  <span class="meta-item">出处：<strong>{{ item.sourceOrg }}</strong></span>
                  <span class="meta-item">发布日期：{{ item.publishDate || '近期' }}</span>
                  <a
                    :href="item.url"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="origin-link"
                    @click.stop
                  >
                    <span>查看原网页</span>
                    <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                      <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
                      <polyline points="15 3 21 3 21 9" />
                      <line x1="10" y1="14" x2="21" y2="3" />
                    </svg>
                  </a>
                </div>
              </div>
              <svg class="expand-arrow-svg" :class="{ 'is-open': isExpanded(item.url) }" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <polyline points="9 18 15 12 9 6" />
              </svg>

              <!-- 展开来源摘要 -->
              <div v-if="isExpanded(item.url)" class="source-summary-drawer" @click.stop>
                <div class="summary-drawer-title">
                  <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                    <polyline points="14 2 14 8 20 8" />
                  </svg>
                  <span>来源摘要（{{ item.title }}）</span>
                </div>
                <p class="summary-drawer-body">{{ item.sourceSummary }}</p>
              </div>
            </div>
          </div>
        </div>

        <div class="conflict-col">
          <div class="conflict-col-title side-b">观点另一方出处</div>
          <div class="evidence-list">
            <div
              v-for="item in opposingEvidences"
              :key="item.url + item.index"
              class="evidence-card"
              @click="toggleExpand(item.url)"
            >
              <div class="evidence-main">
                <div class="quote-row">
                  <span class="stance-badge" :class="getStanceClass(item)">
                    <component :is="getStanceIcon(item)" class="stance-icon-svg" />
                    <span>{{ getStanceBadgeText(item) }}</span>
                  </span>
                  <span class="quote-text">“{{ item.quote || item.title }}”</span>
                </div>
                <div class="meta-row">
                  <span class="meta-item">出处：<strong>{{ item.sourceOrg }}</strong></span>
                  <span class="meta-item">发布日期：{{ item.publishDate || '近期' }}</span>
                  <a
                    :href="item.url"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="origin-link"
                    @click.stop
                  >
                    <span>查看原网页</span>
                    <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                      <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
                      <polyline points="15 3 21 3 21 9" />
                      <line x1="10" y1="14" x2="21" y2="3" />
                    </svg>
                  </a>
                </div>
              </div>
              <svg class="expand-arrow-svg" :class="{ 'is-open': isExpanded(item.url) }" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <polyline points="9 18 15 12 9 6" />
              </svg>

              <div v-if="isExpanded(item.url)" class="source-summary-drawer" @click.stop>
                <div class="summary-drawer-title">
                  <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                    <polyline points="14 2 14 8 20 8" />
                  </svg>
                  <span>来源摘要（{{ item.title }}）</span>
                </div>
                <p class="summary-drawer-body">{{ item.sourceSummary }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 常规出处列表展示（对齐附件图） -->
    <template v-else>
      <div class="evidence-list">
        <div
          v-for="item in combinedList"
          :key="item.url + (item.index || 0)"
          class="evidence-card"
          @click="toggleExpand(item.url)"
        >
          <div class="evidence-main">
            <div class="quote-row">
              <span class="stance-badge" :class="getStanceClass(item)">
                <!-- 矢量图标 -->
                <svg v-if="getStanceClass(item) === 'badge-supported'" viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
                <svg v-else-if="getStanceClass(item) === 'badge-refuted'" viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
                <svg v-else viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="8" x2="12" y2="12" />
                  <line x1="12" y1="16" x2="12.01" y2="16" />
                </svg>
                <span>{{ getStanceBadgeText(item) }}</span>
              </span>

              <span class="quote-text">
                “{{ item.quote || item.title }}”
              </span>
              <span v-if="item.note" class="evidence-note-tag">{{ item.note }}</span>
            </div>

            <div class="meta-row">
              <span class="meta-item">出处：<strong>{{ item.sourceOrg }}</strong></span>
              <span v-if="item.publishDate" class="meta-item">发布日期：{{ item.publishDate }}</span>
              <a
                :href="item.url"
                target="_blank"
                rel="noopener noreferrer"
                class="origin-link"
                @click.stop
              >
                <span>查看原网页</span>
                <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
                  <polyline points="15 3 21 3 21 9" />
                  <line x1="10" y1="14" x2="21" y2="3" />
                </svg>
              </a>
            </div>
          </div>

          <svg class="expand-arrow-svg" :class="{ 'is-open': isExpanded(item.url) }" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <polyline points="9 18 15 12 9 6" />
          </svg>

          <!-- 展开详情面板 -->
          <div v-if="isExpanded(item.url)" class="source-summary-drawer" @click.stop>
            <div class="summary-drawer-title">
              <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <polyline points="14 2 14 8 20 8" />
              </svg>
              <span>来源摘要（{{ item.title }}）</span>
            </div>
            <p class="summary-drawer-body">{{ item.sourceSummary }}</p>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, h } from 'vue'
import type { EvidenceItemDto } from '../types/factcheck'

const props = defineProps<{
  evidences?: EvidenceItemDto[]
  opposingEvidences?: EvidenceItemDto[]
  unverifiedCauseCode?: string
}>()

const expandedUrl = ref<string | null>(null)

const isSourceConflict = computed(() => props.unverifiedCauseCode === 'CONFLICTING_SOURCES')

const hasAnyEvidence = computed(() => {
  return (
    (props.evidences && props.evidences.length > 0) ||
    (props.opposingEvidences && props.opposingEvidences.length > 0)
  )
})

const combinedList = computed(() => {
  const list: EvidenceItemDto[] = []
  if (props.evidences) list.push(...props.evidences)
  if (props.opposingEvidences) list.push(...props.opposingEvidences)
  return list
})

function toggleExpand(url: string) {
  expandedUrl.value = expandedUrl.value === url ? null : url
}

function isExpanded(url: string) {
  return expandedUrl.value === url
}

function getStanceClass(item: EvidenceItemDto) {
  if (item.stanceCode === 'SUPPORTS' || item.stanceCode === 'PARTIALLY_SUPPORTS') {
    return 'badge-supported'
  }
  if (item.stanceCode === 'REFUTES') {
    return 'badge-refuted'
  }
  return 'badge-conditional'
}

function getStanceBadgeText(item: EvidenceItemDto) {
  if (item.stanceLabel) return item.stanceLabel
  if (item.stanceCode === 'SUPPORTS') return '支持'
  if (item.stanceCode === 'PARTIALLY_SUPPORTS') return '可参考'
  if (item.stanceCode === 'REFUTES') return '不支持'
  return '可参考'
}

function getStanceIcon(item: EvidenceItemDto) {
  const cls = getStanceClass(item)
  if (cls === 'badge-supported') {
    return () =>
      h('svg', { viewBox: '0 0 24 24', width: '11', height: '11', fill: 'none', stroke: 'currentColor', 'stroke-width': '3', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [
        h('polyline', { points: '20 6 9 17 4 12' })
      ])
  }
  if (cls === 'badge-refuted') {
    return () =>
      h('svg', { viewBox: '0 0 24 24', width: '11', height: '11', fill: 'none', stroke: 'currentColor', 'stroke-width': '3', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [
        h('line', { x1: '18', y1: '6', x2: '6', y2: '18' }),
        h('line', { x1: '6', y1: '6', x2: '18', y2: '18' })
      ])
  }
  return () =>
    h('svg', { viewBox: '0 0 24 24', width: '11', height: '11', fill: 'none', stroke: 'currentColor', 'stroke-width': '3', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [
      h('circle', { cx: '12', cy: '12', r: '10' }),
      h('line', { x1: '12', y1: '8', x2: '12', y2: '12' }),
      h('line', { x1: '12', y1: '16', x2: '12.01', y2: '16' })
    ])
}
</script>

<style scoped>
.evidence-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 2px;
}

.evidence-heading {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
  letter-spacing: -0.1px;
}

.evidence-list {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.evidence-card {
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 10px;
  padding: 10px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  cursor: pointer;
  position: relative;
  flex-wrap: wrap;
  transition: all 0.15s ease;
}

.evidence-card:hover {
  background: #ffffff;
  border-color: #e2e8f0;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
}

.evidence-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-width: 0;
}

.quote-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.stance-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  font-weight: 700;
  padding: 1.5px 7px;
  border-radius: 4px;
  line-height: 1.3;
}

.stance-icon-svg {
  flex-shrink: 0;
}

.badge-supported {
  background: #ecfdf5;
  color: #059669;
  border: 1px solid #a7f3d0;
}

.badge-refuted {
  background: #fff1f2;
  color: #e11d48;
  border: 1px solid #fecdd3;
}

.badge-conditional {
  background: #fffbeb;
  color: #d97706;
  border: 1px solid #fde68a;
}

.quote-text {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.4;
}

.meta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: #64748b;
}

.meta-item strong {
  color: #334155;
  font-weight: 600;
}

.origin-link {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: #2563eb;
  font-weight: 600;
  text-decoration: none;
  font-size: 12px;
  transition: opacity 0.15s;
}

.origin-link:hover {
  opacity: 0.8;
  text-decoration: underline;
}

.expand-arrow-svg {
  color: #94a3b8;
  flex-shrink: 0;
  transition: transform 0.2s ease;
}

.expand-arrow-svg.is-open {
  transform: rotate(90deg);
}

.source-summary-drawer {
  width: 100%;
  margin-top: 6px;
  padding: 8px 10px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 12px;
  color: #475569;
}

.summary-drawer-title {
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 4px;
}

.summary-drawer-body {
  line-height: 1.5;
}

.conflict-columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.conflict-col-title {
  font-size: 12px;
  font-weight: 700;
  padding: 4px 8px;
  border-radius: 6px;
  margin-bottom: 6px;
}

.conflict-col-title.side-a {
  background: #eff6ff;
  color: #1d4ed8;
}

.conflict-col-title.side-b {
  background: #fff1f2;
  color: #be123c;
}

@media (max-width: 640px) {
  .conflict-columns {
    grid-template-columns: 1fr;
  }
}
</style>
