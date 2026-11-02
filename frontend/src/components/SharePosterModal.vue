<template>
  <div v-if="visible" class="poster-modal-backdrop" @click.self="close">
    <div class="poster-modal-dialog">
      <!-- 弹窗顶部操作区 -->
      <div class="modal-header-bar">
        <div class="header-left">
          <svg class="header-icon" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <rect width="18" height="18" x="3" y="3" rx="2" ry="2"/>
            <circle cx="9" cy="9" r="2"/>
            <path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"/>
          </svg>
          <h3>生成全量辟谣关怀长图</h3>
          <span class="claim-count-pill">共 {{ claims.length }} 条说法</span>
        </div>
        <button type="button" class="btn-close-modal" @click="close" title="关闭">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>

      <!-- 长图预览区域：恢复轻盈浅蓝灰底色，与长图实体形成自然柔和阴影，消除黑底与割裂感 -->
      <div class="poster-scroll-wrapper" ref="scrollWrapperRef">
        <!-- 真正被 html2canvas 截取渲染的长图实体 -->
        <div ref="posterCardRef" class="poster-canvas-card">
          <!-- 1. 海报顶部品牌条：紧凑一行，呈现权威循证来源与核验日期 -->
          <div class="poster-top-header">
            <div class="brand-left">
              <svg class="brand-icon" viewBox="0 0 24 24" width="17" height="17" fill="none">
                <path d="M12 2L14.4 9.6L22 12L14.4 14.4L12 22L9.6 14.4L2 12L9.6 9.6L12 2Z" fill="#2563eb"/>
              </svg>
              <span class="brand-title">家医智核 · 健康辟谣长图</span>
              <span class="wsa-pill">腾讯元宝搜索实时循证</span>
            </div>
            <div class="brand-date">{{ currentDateStr }}</div>
          </div>

          <!-- 2. 整篇核验综述（若存在）：极简淡蓝卡片 -->
          <div v-if="summaryText" class="article-summary-card">
            <div class="summary-card-head">
              <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="#2563eb" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
                <line x1="16" y1="13" x2="8" y2="13"/>
                <line x1="16" y1="17" x2="8" y2="17"/>
              </svg>
              <span>核验综述</span>
            </div>
            <p class="summary-card-body">{{ summaryText }}</p>
          </div>

          <!-- 3. 本次核验的所有说法清单（全量平铺，无多余冗余大标题） -->
          <div class="claims-collection-container">
            <div
              v-for="(item, index) in claims"
              :key="item.claimId || index"
              class="single-claim-poster-card"
              :class="`theme-${(item.verdictCode || 'pending').toLowerCase()}`"
            >
              <!-- 说法序号与分类标签 -->
              <div class="claim-card-head">
                <span class="claim-seq-badge">说法 {{ index + 1 }}</span>
                <span class="claim-category-tag">{{ getCategoryLabel(item.categoryCode) }}</span>
                <span v-if="item.confidenceLabel" class="claim-conf-tag">置信度：{{ item.confidenceLabel }}</span>
              </div>

              <!-- 被查验的原话 -->
              <div class="claim-original-quote">
                <span class="quote-symbol">“</span>{{ item.claimText }}<span class="quote-symbol">”</span>
              </div>

              <!-- 结论与适老化双层印章 -->
              <div class="claim-verdict-row">
                <div class="verdict-explain-col">
                  <div class="verdict-lead-label">核验结论：</div>
                  <p class="verdict-explain-text">
                    {{ item.explanation || '经权威医学资料核实，该说法依据不足或存在健康误导，请谨慎甄别。' }}
                  </p>
                </div>

                <!-- 适老化双层仿真印章 -->
                <div class="poster-stamp-container">
                  <div class="poster-stamp" :class="`stamp-${(item.verdictCode || 'unverified').toLowerCase()}`">
                    <div class="stamp-inner-border">
                      <div class="stamp-sub-text">{{ getStampSubText(item.verdictCode) }}</div>
                      <div class="stamp-main-text">{{ getStampMainText(item.verdictCode) }}</div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 权威医学证据摘录（元宝搜索召回） -->
              <div v-if="item.evidences && item.evidences.length > 0" class="evidence-quote-box">
                <div class="evidence-box-header">
                  <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="#2563eb" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                  </svg>
                  <span>权威医学出处依据：</span>
                </div>

                <div class="evidence-mini-list">
                  <div
                    v-for="(ev, evIdx) in item.evidences.slice(0, 2)"
                    :key="evIdx"
                    class="evidence-mini-item"
                  >
                    <div class="evidence-source-line">
                      <span class="evidence-org-name">{{ ev.sourceOrg || ev.title }}</span>
                      <span class="evidence-tier-tag" :class="ev.tierCode === 'TIER_1' ? 'tier-1' : 'tier-2'">
                        {{ ev.tierCode === 'TIER_1' ? '一级官方' : '权威医学' }}
                      </span>
                    </div>
                    <div v-if="ev.quote" class="evidence-verbatim-text">
                      出处原话：“{{ ev.quote }}”
                    </div>
                    <div v-else class="evidence-summary-text">
                      {{ ev.sourceSummary }}
                    </div>
                  </div>
                </div>
              </div>

              <!-- 药物安全特殊提醒 -->
              <div v-if="item.categoryCode === 'DRUG' || item.drugSafetyNotice" class="drug-notice-line">
                <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="#7c3aed" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="m10.5 20.5 10-10a4.95 4.95 0 1 0-7-7l-10 10a4.95 4.95 0 1 0 7 7Z"/>
                  <path d="m8.5 8.5 7 7"/>
                </svg>
                <span>药物安全警示：请遵医嘱规律服药，切勿自行停药、换药或调整剂量。</span>
              </div>
            </div>
          </div>

          <!-- 4. 底部晚辈关怀温馨叮嘱（紧凑柔和粉框） -->
          <div class="poster-footer-care">
            <div class="care-inner-flex">
              <svg class="heart-icon" viewBox="0 0 24 24" width="15" height="15" fill="#ef4444" stroke="#ef4444" stroke-width="1.5">
                <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/>
              </svg>
              <div class="care-text-wrap">
                <strong class="care-lead-title">晚辈温馨提示：</strong>
                <span class="care-message">身体健康是全家最大的福气！网络偏方秘方切勿盲信盲从，慢性病用药请严格遵照医生医嘱，切勿自行停药或换药。身体不适请及时前往正规医院就医。</span>
              </div>
            </div>
          </div>

          <!-- 5. 底部权威溯源水印 -->
          <div class="poster-watermark">
            <span>本卡片由「家医智核」基于腾讯元宝搜索权威数据核查生成 · 守护家人健康</span>
          </div>
        </div>
      </div>

      <!-- 弹窗底部操作按钮（还原纯净双按钮设计，单行不换行） -->
      <div class="modal-footer-actions">
        <div class="footer-status-hint">
          <span v-if="toastMessage" class="toast-tip-text">{{ toastMessage }}</span>
        </div>
        <div class="actions-buttons-group">
          <button
            type="button"
            class="btn-copy-poster"
            :disabled="generating"
            @click="copyPosterToClipboard"
            title="复制到剪贴板，可直接在微信/微信群发送"
          >
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <rect width="14" height="14" x="8" y="8" rx="2" ry="2"/>
              <path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"/>
            </svg>
            <span>{{ copying ? '正在生成...' : '复制图片（可微信直接粘贴）' }}</span>
          </button>

          <button
            type="button"
            class="btn-download-poster"
            :disabled="generating"
            @click="downloadPoster"
            title="保存高清 PNG 长图到本地相册/文件夹"
          >
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="7 10 12 15 17 10"/>
              <line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
            <span>{{ downloading ? '保存中...' : '保存高清长图' }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import html2canvas from 'html2canvas'
import type { ClaimItemDto } from '../types/factcheck'

const props = defineProps<{
  visible: boolean
  summaryText?: string
  claims: ClaimItemDto[]
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
}>()

const posterCardRef = ref<HTMLElement | null>(null)
const scrollWrapperRef = ref<HTMLElement | null>(null)
const generating = ref(false)
const downloading = ref(false)
const copying = ref(false)
const toastMessage = ref('')

const currentDateStr = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
})

function getCategoryLabel(categoryCode?: string): string {
  switch (categoryCode) {
    case 'DRUG': return '药物与治疗'
    case 'DISEASE': return '急症与疾病'
    case 'LIFESTYLE': return '饮食与生活方式'
    default: return '健康生活'
  }
}

function getStampMainText(verdictCode?: string): string {
  switch (verdictCode) {
    case 'SUPPORTED': return '有依据'
    case 'REFUTED': return '有误导'
    case 'CONDITIONAL': return '看情况'
    case 'UNVERIFIED': return '没查实'
    case 'CONSULT_DOCTOR': return '问医生'
    default: return '已核验'
  }
}

function getStampSubText(verdictCode?: string): string {
  switch (verdictCode) {
    case 'SUPPORTED': return '说法可信'
    case 'REFUTED': return '与依据矛盾'
    case 'CONDITIONAL': return '因人而异'
    case 'UNVERIFIED': return '证据不足'
    case 'CONSULT_DOCTOR': return '需遵医嘱'
    default: return '医学核验'
  }
}

function close() {
  emit('update:visible', false)
}

function showToast(msg: string) {
  toastMessage.value = msg
  setTimeout(() => {
    if (toastMessage.value === msg) {
      toastMessage.value = ''
    }
  }, 4000)
}

async function renderPosterCanvas(): Promise<HTMLCanvasElement | null> {
  const el = posterCardRef.value
  if (!el) return null
  generating.value = true

  // 截图前先将滚动容器复位至顶部，消除视口滚动偏移
  if (scrollWrapperRef.value) {
    scrollWrapperRef.value.scrollTop = 0
  }

  try {
    // 获取元素真实完整的总宽度与总高度（保证长图超长时完整展开）
    const fullWidth = el.scrollWidth || el.offsetWidth || 620
    const fullHeight = Math.max(el.scrollHeight, el.offsetHeight)

    return await html2canvas(el, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#ffffff',
      logging: false,
      scrollX: 0,
      scrollY: 0,
      x: 0,
      y: 0,
      width: fullWidth,
      height: fullHeight,
      // 关键：windowWidth 与 windowHeight 必须覆盖长图全部真实尺寸，否则 iframe 虚拟视口超出部分将丢失背景色
      windowWidth: fullWidth + 100,
      windowHeight: fullHeight + 200,
      onclone: (clonedDoc) => {
        // 确保虚拟文档根元素与 body 撑开至全高度，避免底部背景透明
        if (clonedDoc.documentElement) {
          clonedDoc.documentElement.style.height = `${fullHeight + 200}px`
          clonedDoc.documentElement.style.backgroundColor = '#ffffff'
        }
        if (clonedDoc.body) {
          clonedDoc.body.style.height = `${fullHeight + 200}px`
          clonedDoc.body.style.overflow = 'visible'
          clonedDoc.body.style.backgroundColor = '#ffffff'
        }

        const clonedCard = clonedDoc.querySelector('.poster-canvas-card') as HTMLElement
        if (clonedCard) {
          clonedCard.style.boxShadow = 'none'
          clonedCard.style.margin = '0'
          clonedCard.style.height = `${fullHeight}px`
          clonedCard.style.minHeight = `${fullHeight}px`
          clonedCard.style.maxHeight = 'none'
          clonedCard.style.overflow = 'visible'
          clonedCard.style.backgroundColor = '#ffffff'
        }
      }
    })
  } finally {
    generating.value = false
  }
}

async function downloadPoster() {
  if (generating.value) return
  downloading.value = true
  try {
    const canvas = await renderPosterCanvas()
    if (!canvas) return
    const dataUrl = canvas.toDataURL('image/png')
    const link = document.createElement('a')
    link.href = dataUrl
    link.download = `家医智核辟谣长图_${Date.now()}.png`
    link.click()
    showToast('辟谣长图已开始下载，请在下载目录查看！')
  } catch (err: any) {
    showToast('长图生成失败：' + (err?.message || '未知错误'))
  } finally {
    downloading.value = false
  }
}

async function copyPosterToClipboard() {
  if (generating.value) return
  copying.value = true
  try {
    const canvas = await renderPosterCanvas()
    if (!canvas) return
    canvas.toBlob(async (blob) => {
      if (!blob) {
        showToast('生成图片数据失败')
        copying.value = false
        return
      }
      try {
        if (navigator.clipboard && navigator.clipboard.write) {
          await navigator.clipboard.write([
            new ClipboardItem({ 'image/png': blob })
          ])
          showToast('整张辟谣长图已复制到剪贴板！可直接在微信/微信群 Ctrl+V 粘贴发送！')
        } else {
          showToast('当前浏览器不支持直接写入剪贴板图片，请使用“保存高清长图”按钮！')
        }
      } catch (e: any) {
        showToast('剪贴板复制受限，请点击右侧“保存高清长图”直接下载图片')
      } finally {
        copying.value = false
      }
    }, 'image/png')
  } catch (err: any) {
    showToast('生成海报失败：' + (err?.message || '未知错误'))
    copying.value = false
  }
}
</script>

<style scoped>
.poster-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(15, 23, 42, 0.65);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.poster-modal-dialog {
  background: #ffffff;
  border-radius: 16px;
  width: 100%;
  max-width: 640px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 40px -10px rgba(15, 23, 42, 0.25);
  overflow: hidden;
}

/* 弹窗顶部标头（精致轻盈） */
.modal-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 18px;
  border-bottom: 1px solid #f1f5f9;
  background: #ffffff;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  color: #2563eb;
}

.header-left h3 {
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.claim-count-pill {
  font-size: 11px;
  font-weight: 700;
  background: #eff6ff;
  color: #2563eb;
  padding: 1px 7px;
  border-radius: 999px;
  border: 1px solid #bfdbfe;
}

.btn-close-modal {
  border: none;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-close-modal:hover {
  background: #f1f5f9;
  color: #0f172a;
}

/* 预览滚动区域：淡灰蓝背景，长图边缘自然立体 */
.poster-scroll-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 14px 16px;
  background: #f1f5f9;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

/* 长图实体卡片（被 html2canvas 渲染）：洁白底色、紧凑内边距、消除多余大留白 */
.poster-canvas-card {
  width: 580px;
  max-width: 100%;
  height: auto;
  min-height: min-content;
  flex-shrink: 0;
  align-self: flex-start;
  background: #ffffff;
  border-radius: 14px;
  padding: 16px 18px 12px;
  box-shadow: 0 4px 20px -2px rgba(15, 23, 42, 0.08);
  border: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  box-sizing: border-box;
}

/* 1. 顶部品牌条：紧凑一行 */
.poster-top-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1.5px solid #f1f5f9;
}

.brand-left {
  display: flex;
  align-items: center;
  gap: 6px;
}

.brand-title {
  font-size: 15px;
  font-weight: 900;
  color: #1e3a8a;
  letter-spacing: 0.2px;
}

.wsa-pill {
  font-size: 10px;
  font-weight: 700;
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  padding: 1px 6px;
  border-radius: 4px;
}

.brand-date {
  font-size: 11px;
  color: #64748b;
  font-weight: 600;
}

/* 2. 整篇核验综述 */
.article-summary-card {
  background: #f0f7ff;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  padding: 8px 12px;
}

.summary-card-head {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 800;
  color: #1d4ed8;
  margin-bottom: 4px;
}

.summary-card-body {
  font-size: 12.5px;
  color: #1e293b;
  line-height: 1.5;
  margin: 0;
  font-weight: 500;
}

/* 3. 说法列表容器 */
.claims-collection-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 单条论断卡片 */
.single-claim-poster-card {
  background: #ffffff;
  border-radius: 10px;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.single-claim-poster-card.theme-refuted {
  border-left: 4px solid #dc2626;
}
.single-claim-poster-card.theme-supported {
  border-left: 4px solid #16a34a;
}
.single-claim-poster-card.theme-conditional {
  border-left: 4px solid #d97706;
}
.single-claim-poster-card.theme-unverified {
  border-left: 4px solid #2563eb;
}
.single-claim-poster-card.theme-consult_doctor {
  border-left: 4px solid #7c3aed;
}

.claim-card-head {
  display: flex;
  align-items: center;
  gap: 6px;
}

.claim-seq-badge {
  font-size: 10px;
  font-weight: 800;
  background: #0f172a;
  color: #ffffff;
  padding: 1px 6px;
  border-radius: 4px;
}

.claim-category-tag {
  font-size: 10.5px;
  font-weight: 700;
  background: #f1f5f9;
  color: #475569;
  padding: 1px 6px;
  border-radius: 4px;
}

.claim-conf-tag {
  font-size: 10.5px;
  font-weight: 600;
  background: #f8fafc;
  color: #64748b;
  padding: 1px 6px;
  border-radius: 4px;
  border: 1px solid #e2e8f0;
}

.claim-original-quote {
  font-size: 14.5px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.35;
  padding-left: 2px;
}

.quote-symbol {
  color: #64748b;
  font-family: Georgia, serif;
}

/* 结论与印章横排 */
.claim-verdict-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  background: #f8fafc;
  border-radius: 8px;
  padding: 8px 10px;
  border: 1px solid #e2e8f0;
}

.verdict-explain-col {
  flex: 1;
}

.verdict-lead-label {
  font-size: 11px;
  font-weight: 800;
  color: #475569;
  margin-bottom: 2px;
}

.verdict-explain-text {
  font-size: 12.5px;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.45;
  margin: 0;
}

/* 适老化仿真印章（精致 68px） */
.poster-stamp-container {
  flex-shrink: 0;
}

.poster-stamp {
  width: 68px;
  height: 68px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transform: rotate(-10deg);
  border: 2.5px solid;
  padding: 2.5px;
  box-sizing: border-box;
  background: #ffffff;
}

.stamp-inner-border {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 1.2px dashed;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}

.stamp-sub-text {
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.stamp-main-text {
  font-size: 14px;
  font-weight: 900;
  letter-spacing: 0.8px;
  margin-top: 1px;
}

.stamp-refuted {
  border-color: #dc2626;
  color: #dc2626;
}
.stamp-refuted .stamp-inner-border {
  border-color: #dc2626;
}

.stamp-supported {
  border-color: #16a34a;
  color: #16a34a;
}
.stamp-supported .stamp-inner-border {
  border-color: #16a34a;
}

.stamp-conditional {
  border-color: #d97706;
  color: #d97706;
}
.stamp-conditional .stamp-inner-border {
  border-color: #d97706;
}

.stamp-unverified {
  border-color: #2563eb;
  color: #2563eb;
}
.stamp-unverified .stamp-inner-border {
  border-color: #2563eb;
}

.stamp-consult_doctor {
  border-color: #7c3aed;
  color: #7c3aed;
}
.stamp-consult_doctor .stamp-inner-border {
  border-color: #7c3aed;
}

/* 证据出处与原话 */
.evidence-quote-box {
  display: flex;
  flex-direction: column;
  gap: 5px;
  background: #f8fafc;
  border-radius: 8px;
  padding: 8px 10px;
  border: 1px solid #e2e8f0;
}

.evidence-box-header {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11.5px;
  font-weight: 700;
  color: #334155;
}

.evidence-mini-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.evidence-mini-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.evidence-source-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.evidence-org-name {
  font-size: 11.5px;
  font-weight: 700;
  color: #1e293b;
}

.evidence-tier-tag {
  font-size: 9.5px;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 4px;
}

.evidence-tier-tag.tier-1 {
  background: #dbeafe;
  color: #1d4ed8;
}

.evidence-tier-tag.tier-2 {
  background: #e2e8f0;
  color: #475569;
}

.evidence-verbatim-text {
  font-size: 11.5px;
  color: #1e293b;
  line-height: 1.45;
  background: #ffffff;
  padding: 5px 8px;
  border-radius: 6px;
  border-left: 3px solid #2563eb;
  font-style: italic;
  font-weight: 500;
}

.evidence-summary-text {
  font-size: 11.5px;
  color: #475569;
  line-height: 1.45;
}

.drug-notice-line {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: #6b21a8;
  font-weight: 600;
  background: #faf5ff;
  padding: 5px 8px;
  border-radius: 6px;
  border: 1px solid #f3e8ff;
}

/* 4. 底部晚辈关怀（紧凑粉框） */
.poster-footer-care {
  background: #fff1f2;
  border: 1px solid #fecdd3;
  border-radius: 8px;
  padding: 8px 12px;
}

.care-inner-flex {
  display: flex;
  align-items: flex-start;
  gap: 6px;
}

.heart-icon {
  flex-shrink: 0;
  margin-top: 1px;
}

.care-text-wrap {
  font-size: 11px;
  line-height: 1.45;
  color: #881337;
}

.care-lead-title {
  font-weight: 800;
  color: #be123c;
}

.care-message {
  font-weight: 600;
}

/* 5. 权威溯源水印 */
.poster-watermark {
  text-align: center;
  font-size: 10px;
  color: #94a3b8;
  padding: 0;
  margin-top: -2px;
}

/* 弹窗底部操作栏（清爽双按钮、单行不折行、紧凑优雅） */
.modal-footer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 10px 18px;
  border-top: 1px solid #f1f5f9;
  background: #ffffff;
  flex-shrink: 0;
  gap: 12px;
}

.footer-status-hint {
  flex: 1;
}

.toast-tip-text {
  font-size: 12px;
  font-weight: 700;
  color: #16a34a;
}

.actions-buttons-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-copy-poster {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}

.btn-copy-poster:hover:not(:disabled) {
  background: #dbeafe;
}

.btn-download-poster {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
  color: #ffffff;
  background: #2563eb;
  border: none;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.2);
  white-space: nowrap;
  transition: all 0.2s;
}

.btn-download-poster:hover:not(:disabled) {
  background: #1d4ed8;
}

.btn-copy-poster:disabled,
.btn-download-poster:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
