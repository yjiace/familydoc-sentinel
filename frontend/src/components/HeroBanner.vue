<template>
  <!-- 顶部品牌横幅：紧凑型布局，1:1 对齐原型图设计 -->
  <header class="hero-banner">
    <div class="hero-left">
      <div class="brand-row">
        <!-- 蓝色圆角盾牌对勾 Logo -->
        <div class="brand-logo" aria-hidden="true">
          <svg viewBox="0 0 48 48" width="40" height="40" fill="none">
            <rect width="48" height="48" rx="13" fill="url(#heroLogoGrad)" />
            <path
              d="M24 9.5L12.5 14.5V23.5C12.5 31.2 17.4 38.2 24 40.5C30.6 38.2 35.5 31.2 35.5 23.5V14.5L24 9.5Z"
              stroke="#FFFFFF"
              stroke-width="2.6"
              fill="rgba(255,255,255,0.18)"
            />
            <path
              d="M19 24.5L22.5 28L29.5 20.5"
              stroke="#FFFFFF"
              stroke-width="3"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
            <defs>
              <linearGradient id="heroLogoGrad" x1="0" y1="0" x2="48" y2="48" gradientUnits="userSpaceOnUse">
                <stop stop-color="#3b82f6" />
                <stop offset="1" stop-color="#1d4ed8" />
              </linearGradient>
            </defs>
          </svg>
        </div>

        <div class="brand-titles">
          <h1 class="main-title">
            <span class="title-dark">家医智核</span>
            <span class="title-dot">·</span>
            <span class="title-blue">流言粉碎</span>
          </h1>
          <p class="sub-title">基于权威医学信息，快速识别健康流言，守护您和家人的健康</p>
        </div>
      </div>

      <!-- 三大核心特性微徽标 -->
      <div class="feature-badges">
        <div class="feature-item">
          <div class="feature-icon icon-bolt" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
              <path d="M13 2L3 14H12L11 22L21 10H12L13 2Z" />
            </svg>
          </div>
          <div class="feature-text">
            <div class="feature-name">权威信息源</div>
            <div class="feature-desc">来自官方机构和医学文献</div>
          </div>
        </div>

        <div class="feature-item">
          <div class="feature-icon icon-shield" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none">
              <path
                d="M12 2L4 5.6V11.8C4 17.2 7.4 22.1 12 23.8C16.6 22.1 20 17.2 20 11.8V5.6L12 2Z"
                fill="currentColor"
              />
              <path
                d="M9 12.2L11.1 14.3L15.4 10"
                stroke="#FFFFFF"
                stroke-width="2.2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </div>
          <div class="feature-text">
            <div class="feature-name">AI 智能核验</div>
            <div class="feature-desc">多维度交叉验证</div>
          </div>
        </div>

        <div class="feature-item">
          <div class="feature-icon icon-speed" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none">
              <path
                d="M12 3C7.03 3 3 7.03 3 12C3 15.69 5.22 18.86 8.42 20.24"
                stroke="currentColor"
                stroke-width="2.2"
                stroke-linecap="round"
              />
              <path
                d="M15.58 20.24C18.78 18.86 21 15.69 21 12C21 7.03 16.97 3 12 3"
                stroke="currentColor"
                stroke-width="2.2"
                stroke-linecap="round"
              />
              <path
                d="M12 7V12L15.5 15.5"
                stroke="currentColor"
                stroke-width="2.2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </div>
          <div class="feature-text">
            <div class="feature-name">快速准确</div>
            <div class="feature-desc">秒级返回结果</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧：艺术风标语与 3D 风格防护盾牌放大镜插画及用户状态区 -->
    <div class="hero-right">
      <div class="slogan-art" aria-hidden="true">
        <div class="slogan-line1">科学甄别流言</div>
        <div class="slogan-line2">让健康信息更可信，</div>
      </div>

      <div class="hero-illustration" aria-hidden="true">
        <svg viewBox="0 0 220 110" width="160" height="80" fill="none">
          <ellipse cx="115" cy="85" rx="75" ry="18" fill="url(#heroRingGrad)" />
          <path d="M115 12L80 24V54C80 78.5 95 99.5 115 106C135 99.5 150 78.5 150 54V24L115 12Z" fill="url(#heroShieldOuter)" />
          <path d="M115 18L86 28.5V53.5C86 74 98.5 91.5 115 97.5C131.5 91.5 144 74 144 53.5V28.5L115 18Z" fill="url(#heroShieldInner)" />
          <path d="M103 55L111 63L127 46" stroke="#FFFFFF" stroke-width="4.5" stroke-linecap="round" stroke-linejoin="round" />
          <defs>
            <radialGradient id="heroRingGrad" cx="0.5" cy="0.5" r="0.5">
              <stop stop-color="#BFDBFE" stop-opacity="0.7" />
              <stop offset="1" stop-color="#BFDBFE" stop-opacity="0" />
            </radialGradient>
            <linearGradient id="heroShieldOuter" x1="80" y1="12" x2="150" y2="106" gradientUnits="userSpaceOnUse">
              <stop stop-color="#60A5FA" />
              <stop offset="1" stop-color="#1D4ED8" />
            </linearGradient>
            <linearGradient id="heroShieldInner" x1="86" y1="18" x2="144" y2="97.5" gradientUnits="userSpaceOnUse">
              <stop stop-color="#3B82F6" />
              <stop offset="1" stop-color="#2563EB" />
            </linearGradient>
          </defs>
        </svg>
      </div>

      <!-- 用户状态控制入口 -->
      <div class="user-auth-zone">
        <!-- 未登录状态 -->
        <button
          v-if="!isLoggedIn"
          type="button"
          class="login-entry-btn"
          @click="openAuthModal('login')"
        >
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
            <circle cx="12" cy="7" r="4" />
          </svg>
          <span>登录 / 注册</span>
        </button>

        <!-- 已登录状态 -->
        <div v-else class="user-logged-profile" ref="dropdownRef">
          <div class="user-pill-card" :class="{ 'is-quota-exceeded': userUsage?.quotaExceeded }" @click.stop="toggleDropdown">
            <div class="user-avatar-badge" :class="{ 'avatar-exceeded': userUsage?.quotaExceeded }">
              {{ (currentUser?.nickname || currentUser?.email || '用')[0].toUpperCase() }}
            </div>
            <div class="user-info-text">
              <div class="user-name-row">
                <span class="user-name-title">{{ currentUser?.nickname || currentUser?.email }}</span>
                <span v-if="userUsage?.quotaExceeded" class="pill-stop-tag">停止服务</span>
              </div>
              <div class="user-usage-micro">
                <span class="usage-token-badge" :class="{ 'text-danger': isLimitReached(userUsage?.dailyTokensUsed, userUsage?.dailyTokenLimit) }">
                  今日Token: {{ formatTokens(userUsage?.dailyTokensUsed || 0) }}/{{ formatTokens(userUsage?.dailyTokenLimit || 50000) }}
                </span>
                <span class="usage-dot">·</span>
                <span class="usage-search-badge" :class="{ 'text-danger': isLimitReached(userUsage?.dailySearchUsed, userUsage?.dailySearchLimit) }">
                  搜索: {{ userUsage?.dailySearchUsed || 0 }}/{{ userUsage?.dailySearchLimit || 30 }}次
                </span>
              </div>
            </div>
            <svg
              class="dropdown-chevron"
              :class="{ 'is-open': isDropdownOpen }"
              viewBox="0 0 24 24"
              width="14"
              height="14"
              stroke="currentColor"
              stroke-width="2"
              fill="none"
            >
              <polyline points="6 9 12 15 18 9" />
            </svg>
          </div>

          <!-- 用户下拉卡片 -->
          <div v-if="isDropdownOpen" class="user-dropdown-menu" @click.stop>
            <div class="dropdown-header">
              <div class="dropdown-email" :title="currentUser?.email">{{ currentUser?.email }}</div>
              <div class="dropdown-role-tag" :class="{ 'tag-danger': userUsage?.quotaExceeded }">
                {{ userUsage?.quotaExceeded ? '额度已满' : '服务正常' }}
              </div>
            </div>

            <!-- 额度耗尽停止服务告警 -->
            <div v-if="userUsage?.quotaExceeded" class="quota-exceeded-banner">
              {{ userUsage.quotaExceededReason || '您的双链额度已用完，已暂停核验服务' }}
            </div>

            <!-- 双链三周期额度详情 -->
            <div class="quota-periods-section">
              <div class="quota-section-title">双链额度用量（满额即停）</div>

              <!-- 每日额度 -->
              <div class="quota-period-card">
                <div class="period-header">
                  <span class="period-badge">每日额度</span>
                  <span
                    class="period-status"
                    :class="isPeriodExceeded('daily') ? 'status-stop' : 'status-ok'"
                  >
                    {{ isPeriodExceeded('daily') ? '已满额' : '正常' }}
                  </span>
                </div>
                <div class="chain-row">
                  <div class="chain-label-line">
                    <span>Token 消耗</span>
                    <span class="chain-nums">{{ formatTokens(userUsage?.dailyTokensUsed || 0) }} / {{ formatTokens(userUsage?.dailyTokenLimit || 50000) }}</span>
                  </div>
                  <div class="chain-progress-track">
                    <div
                      class="chain-progress-fill fill-token"
                      :class="getProgressColorClass(userUsage?.dailyTokensUsed, userUsage?.dailyTokenLimit)"
                      :style="{ width: getProgressPercent(userUsage?.dailyTokensUsed, userUsage?.dailyTokenLimit) + '%' }"
                    ></div>
                  </div>
                </div>
                <div class="chain-row">
                  <div class="chain-label-line">
                    <span>元宝搜索</span>
                    <span class="chain-nums">{{ userUsage?.dailySearchUsed || 0 }} / {{ userUsage?.dailySearchLimit || 30 }} 次</span>
                  </div>
                  <div class="chain-progress-track">
                    <div
                      class="chain-progress-fill fill-search"
                      :class="getProgressColorClass(userUsage?.dailySearchUsed, userUsage?.dailySearchLimit)"
                      :style="{ width: getProgressPercent(userUsage?.dailySearchUsed, userUsage?.dailySearchLimit) + '%' }"
                    ></div>
                  </div>
                </div>
              </div>

              <!-- 每周额度 -->
              <div class="quota-period-card">
                <div class="period-header">
                  <span class="period-badge">每周额度</span>
                  <span
                    class="period-status"
                    :class="isPeriodExceeded('weekly') ? 'status-stop' : 'status-ok'"
                  >
                    {{ isPeriodExceeded('weekly') ? '已满额' : '正常' }}
                  </span>
                </div>
                <div class="chain-row">
                  <div class="chain-label-line">
                    <span>Token 消耗</span>
                    <span class="chain-nums">{{ formatTokens(userUsage?.weeklyTokensUsed || 0) }} / {{ formatTokens(userUsage?.weeklyTokenLimit || 200000) }}</span>
                  </div>
                  <div class="chain-progress-track">
                    <div
                      class="chain-progress-fill fill-token"
                      :class="getProgressColorClass(userUsage?.weeklyTokensUsed, userUsage?.weeklyTokenLimit)"
                      :style="{ width: getProgressPercent(userUsage?.weeklyTokensUsed, userUsage?.weeklyTokenLimit) + '%' }"
                    ></div>
                  </div>
                </div>
                <div class="chain-row">
                  <div class="chain-label-line">
                    <span>元宝搜索</span>
                    <span class="chain-nums">{{ userUsage?.weeklySearchUsed || 0 }} / {{ userUsage?.weeklySearchLimit || 120 }} 次</span>
                  </div>
                  <div class="chain-progress-track">
                    <div
                      class="chain-progress-fill fill-search"
                      :class="getProgressColorClass(userUsage?.weeklySearchUsed, userUsage?.weeklySearchLimit)"
                      :style="{ width: getProgressPercent(userUsage?.weeklySearchUsed, userUsage?.weeklySearchLimit) + '%' }"
                    ></div>
                  </div>
                </div>
              </div>

              <!-- 每月额度 -->
              <div class="quota-period-card">
                <div class="period-header">
                  <span class="period-badge">每月额度</span>
                  <span
                    class="period-status"
                    :class="isPeriodExceeded('monthly') ? 'status-stop' : 'status-ok'"
                  >
                    {{ isPeriodExceeded('monthly') ? '已满额' : '正常' }}
                  </span>
                </div>
                <div class="chain-row">
                  <div class="chain-label-line">
                    <span>Token 消耗</span>
                    <span class="chain-nums">{{ formatTokens(userUsage?.monthlyTokensUsed || 0) }} / {{ formatTokens(userUsage?.monthlyTokenLimit || 500000) }}</span>
                  </div>
                  <div class="chain-progress-track">
                    <div
                      class="chain-progress-fill fill-token"
                      :class="getProgressColorClass(userUsage?.monthlyTokensUsed, userUsage?.monthlyTokenLimit)"
                      :style="{ width: getProgressPercent(userUsage?.monthlyTokensUsed, userUsage?.monthlyTokenLimit) + '%' }"
                    ></div>
                  </div>
                </div>
                <div class="chain-row">
                  <div class="chain-label-line">
                    <span>元宝搜索</span>
                    <span class="chain-nums">{{ userUsage?.monthlySearchUsed || 0 }} / {{ userUsage?.monthlySearchLimit || 300 }} 次</span>
                  </div>
                  <div class="chain-progress-track">
                    <div
                      class="chain-progress-fill fill-search"
                      :class="getProgressColorClass(userUsage?.monthlySearchUsed, userUsage?.monthlySearchLimit)"
                      :style="{ width: getProgressPercent(userUsage?.monthlySearchUsed, userUsage?.monthlySearchLimit) + '%' }"
                    ></div>
                  </div>
                </div>
              </div>
            </div>

            <div class="dropdown-stats-panel">
              <div class="stat-item">
                <div class="stat-num">{{ formatTokens(userUsage?.totalTokens || 0) }}</div>
                <div class="stat-label">累计 Token</div>
              </div>
              <div class="stat-item">
                <div class="stat-num">{{ userUsage?.totalSearchCount || 0 }}</div>
                <div class="stat-label">累计搜索</div>
              </div>
              <div class="stat-item">
                <div class="stat-num">{{ userUsage?.taskCount || 0 }}</div>
                <div class="stat-label">核验任务</div>
              </div>
            </div>
            <div class="dropdown-divider"></div>
            <button type="button" class="dropdown-action-btn" @click="handleChangePassword">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
              <span>修改密码</span>
            </button>
            <button type="button" class="dropdown-action-btn logout-action" @click="handleLogout">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                <polyline points="16 17 21 12 16 7" />
                <line x1="21" y1="12" x2="9" y2="12" />
              </svg>
              <span>退出登录</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import {
  currentUser,
  userUsage,
  isLoggedIn,
  openAuthModal,
  logout
} from '../utils/userState'

const isDropdownOpen = ref<boolean>(false)
const dropdownRef = ref<HTMLElement | null>(null)

function toggleDropdown(): void {
  isDropdownOpen.value = !isDropdownOpen.value
}

function handleLogout(): void {
  isDropdownOpen.value = false
  logout()
}

function handleChangePassword(): void {
  isDropdownOpen.value = false
  openAuthModal('change_password')
}

function onGlobalClick(e: MouseEvent): void {
  if (dropdownRef.value && !dropdownRef.value.contains(e.target as Node)) {
    isDropdownOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', onGlobalClick)
})

onUnmounted(() => {
  document.removeEventListener('click', onGlobalClick)
})

function formatTokens(num: number): string {
  if (num >= 10000) {
    return (num / 1000).toFixed(1).replace(/\.0$/, '') + 'k'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1).replace(/\.0$/, '') + 'k'
  }
  return num.toString()
}

function isLimitReached(used?: number, limit?: number): boolean {
  if (!limit || limit <= 0) return false
  return (used || 0) >= limit
}

function isPeriodExceeded(period: 'daily' | 'weekly' | 'monthly'): boolean {
  const u = userUsage.value
  if (!u) return false
  if (period === 'daily') {
    return isLimitReached(u.dailyTokensUsed, u.dailyTokenLimit) || isLimitReached(u.dailySearchUsed, u.dailySearchLimit)
  }
  if (period === 'weekly') {
    return isLimitReached(u.weeklyTokensUsed, u.weeklyTokenLimit) || isLimitReached(u.weeklySearchUsed, u.weeklySearchLimit)
  }
  return isLimitReached(u.monthlyTokensUsed, u.monthlyTokenLimit) || isLimitReached(u.monthlySearchUsed, u.monthlySearchLimit)
}

function getProgressPercent(used?: number, limit?: number): number {
  if (!limit || limit <= 0) return 0
  const pct = Math.round(((used || 0) / limit) * 100)
  return Math.max(0, Math.min(100, pct))
}

function getProgressColorClass(used?: number, limit?: number): string {
  const pct = getProgressPercent(used, limit)
  if (pct >= 100) return 'bar-danger'
  if (pct >= 80) return 'bar-warn'
  return ''
}
</script>

<style scoped>
.hero-banner {
  max-width: 1400px;
  /* margin: 0 auto; */
  padding: 10px 28px 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-shrink: 0;
}

.hero-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-logo {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  filter: drop-shadow(0 4px 10px rgba(37, 99, 235, 0.28));
}

.brand-titles {
  display: flex;
  flex-direction: column;
}

.main-title {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 24px;
  font-weight: 800;
  line-height: 1.25;
  letter-spacing: -0.2px;
}

.title-dark {
  color: #0f172a;
}

.title-dot {
  color: #94a3b8;
  font-weight: 400;
}

.title-blue {
  color: #2563eb;
}

.sub-title {
  font-size: 13px;
  color: #64748b;
  margin-top: 1px;
}

.feature-badges {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-top: 2px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.feature-icon {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.icon-bolt {
  background: #eff6ff;
  color: #2563eb;
}

.icon-shield {
  background: #eff6ff;
  color: #2563eb;
}

.icon-speed {
  background: #eff6ff;
  color: #0284c7;
}

.feature-text {
  display: flex;
  flex-direction: column;
}

.feature-name {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
}

.feature-desc {
  font-size: 11px;
  color: #64748b;
  line-height: 1.2;
}

.hero-right {
  display: flex;
  align-items: center;
  gap: 12px;
  user-select: none;
}

.slogan-art {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  color: #64748b;
  font-family: 'Kaiti SC', 'STKaiti', 'KaiTi', 'PingFang SC', cursive, sans-serif;
  font-style: italic;
  font-size: 15px;
  line-height: 1.35;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.hero-illustration {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.user-auth-zone {
  margin-left: 6px;
  position: relative;
}

.login-entry-btn {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 8px 16px;
  border-radius: 20px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #2563eb;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
}

.login-entry-btn:hover {
  background: #2563eb;
  color: #ffffff;
  border-color: #2563eb;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.22);
}

.user-logged-profile {
  position: relative;
  cursor: pointer;
}

.user-pill-card {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 5px 12px 5px 6px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 22px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.05);
  transition: all 0.15s ease;
}

.user-pill-card.is-quota-exceeded {
  border-color: #fca5a5;
  background: #fff5f5;
}

.user-pill-card:hover {
  border-color: #cbd5e1;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
}

.user-avatar-badge {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-avatar-badge.avatar-exceeded {
  background: linear-gradient(135deg, #ef4444, #b91c1c);
}

.user-info-text {
  display: flex;
  flex-direction: column;
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.user-name-title {
  font-size: 12.5px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.2;
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pill-stop-tag {
  font-size: 9.5px;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 6px;
  background: #fee2e2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.user-usage-micro {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 10.5px;
  color: #64748b;
  margin-top: 2px;
}

.usage-token-badge {
  color: #2563eb;
  font-weight: 600;
}

.usage-search-badge {
  color: #0284c7;
  font-weight: 600;
}

.text-danger {
  color: #dc2626 !important;
  font-weight: 700;
}

.dropdown-chevron {
  color: #94a3b8;
  transition: transform 0.2s ease;
}

.dropdown-chevron.is-open {
  transform: rotate(180deg);
}

.user-dropdown-menu {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  width: 288px;
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 14px 34px -4px rgba(15, 23, 42, 0.16);
  padding: 12px;
  z-index: 100;
  animation: fadeIn 0.15s ease-out;
}

.user-dropdown-menu::before {
  content: '';
  position: absolute;
  top: -8px;
  left: 0;
  right: 0;
  height: 8px;
}

.dropdown-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  margin-bottom: 8px;
}

.dropdown-email {
  font-size: 12px;
  color: #475569;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dropdown-role-tag {
  font-size: 10px;
  padding: 2px 7px;
  border-radius: 10px;
  background: #ecfdf5;
  color: #059669;
  font-weight: 700;
  white-space: nowrap;
}

.dropdown-role-tag.tag-danger {
  background: #fef2f2;
  color: #dc2626;
}

.quota-exceeded-banner {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #b91c1c;
  font-size: 11px;
  font-weight: 600;
  line-height: 1.4;
  padding: 7px 9px;
  border-radius: 8px;
  margin-bottom: 8px;
}

.quota-periods-section {
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin-bottom: 8px;
}

.quota-section-title {
  font-size: 11px;
  font-weight: 700;
  color: #475569;
}

.quota-period-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 9px;
  padding: 7px 9px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.period-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.period-badge {
  font-size: 11px;
  font-weight: 700;
  color: #1e293b;
}

.period-status {
  font-size: 10px;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 4px;
}

.status-ok {
  background: #eff6ff;
  color: #2563eb;
}

.status-stop {
  background: #fee2e2;
  color: #dc2626;
}

.chain-row {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.chain-label-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 10.5px;
  color: #64748b;
}

.chain-nums {
  font-weight: 600;
  color: #334155;
  font-variant-numeric: tabular-nums;
}

.chain-progress-track {
  width: 100%;
  height: 5px;
  background: #e2e8f0;
  border-radius: 999px;
  overflow: hidden;
}

.chain-progress-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.25s ease;
}

.fill-token {
  background: #3b82f6;
}

.fill-search {
  background: #0ea5e9;
}

.bar-warn {
  background: #f59e0b !important;
}

.bar-danger {
  background: #ef4444 !important;
}

.dropdown-stats-panel {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  background: #f8fafc;
  border-radius: 8px;
  padding: 7px 4px;
  text-align: center;
}

.stat-item {
  display: flex;
  flex-direction: column;
}

.stat-num {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
}

.stat-label {
  font-size: 10px;
  color: #94a3b8;
}

.dropdown-divider {
  height: 1px;
  background: #f1f5f9;
  margin: 10px 0;
}

.dropdown-action-btn {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  border-radius: 6px;
  border: none;
  background: transparent;
  font-size: 12.5px;
  cursor: pointer;
  color: #475569;
  transition: all 0.12s ease;
}

.logout-action:hover {
  background: #fef2f2;
  color: #dc2626;
}

@media (max-width: 900px) {
  .hero-right .slogan-art,
  .hero-right .hero-illustration {
    display: none;
  }
  .feature-badges {
    flex-wrap: wrap;
    gap: 12px;
  }
}
</style>
