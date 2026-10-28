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

    <!-- 右侧：艺术风标语与 3D 风格防护盾牌放大镜插画 -->
    <div class="hero-right" aria-hidden="true">
      <div class="slogan-art">
        <div class="slogan-line1">科学甄别流言</div>
        <div class="slogan-line2">让健康信息更可信，</div>
      </div>

      <div class="hero-illustration">
        <svg viewBox="0 0 220 110" width="200" height="100" fill="none">
          <!-- 底部微光光斑 -->
          <ellipse cx="115" cy="85" rx="75" ry="18" fill="url(#heroRingGrad)" />

          <!-- 漂浮医疗小文献片 -->
          <rect x="20" y="24" width="22" height="24" rx="5" fill="#DBEAFE" opacity="0.85" />
          <path d="M26 31H36M26 36H33" stroke="#3B82F6" stroke-width="1.8" stroke-linecap="round" />
          <rect x="180" y="22" width="20" height="22" rx="5" fill="#E0F2FE" opacity="0.9" />
          <path d="M185 32H195M190 27V37" stroke="#0284C7" stroke-width="1.8" stroke-linecap="round" />

          <!-- 主体质感盾牌 -->
          <path
            d="M115 12L80 24V54C80 78.5 95 99.5 115 106C135 99.5 150 78.5 150 54V24L115 12Z"
            fill="url(#heroShieldOuter)"
          />
          <path
            d="M115 18L86 28.5V53.5C86 74 98.5 91.5 115 97.5C131.5 91.5 144 74 144 53.5V28.5L115 18Z"
            fill="url(#heroShieldInner)"
          />
          <path
            d="M103 55L111 63L127 46"
            stroke="#FFFFFF"
            stroke-width="4.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />

          <!-- 放大镜微插画 -->
          <g transform="translate(138, 55)">
            <circle cx="15" cy="15" r="14" stroke="#2563EB" stroke-width="3" fill="#FFFFFF" fill-opacity="0.9" />
            <path d="M26 26L36 36" stroke="#2563EB" stroke-width="3.5" stroke-linecap="round" />
            <circle cx="15" cy="15" r="10" fill="#93C5FD" fill-opacity="0.35" />
          </g>

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
    </div>
  </header>
</template>

<script setup lang="ts">
/**
 * 顶部品牌横幅组件 HeroBanner
 */
</script>

<style scoped>
.hero-banner {
  max-width: 1400px;
  //margin: 0 auto;
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

@media (max-width: 900px) {
  .hero-right {
    display: none;
  }
  .feature-badges {
    flex-wrap: wrap;
    gap: 12px;
  }
}
</style>
