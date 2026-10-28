<template>
  <!-- 首次使用知情须知确认弹窗：纯 SVG 矢量图标 -->
  <div v-if="visible" class="modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="consent-title">
    <div class="modal-card">
      <div class="modal-header">
        <div class="shield-badge-box" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#2563eb" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
            <path d="m9 12 2 2 4-4" />
          </svg>
        </div>
        <h3 id="consent-title">使用前知情须知</h3>
      </div>

      <div class="modal-body">
        <p class="intro-line">欢迎使用 <strong>家医智核·流言粉碎</strong>。首次核验前请您了解并确认以下事项：</p>
        <ul class="consent-list">
          <li>
            <strong>健康信息比对工具，非医疗服务：</strong>
            本工具仅依据公开权威来源对健康说法进行比对，不提供个体化用药、停药或诊疗建议。
          </li>
          <li>
            <strong>不能用于判断紧急情况：</strong>
            如您或家人正出现胸痛、呼吸困难、昏迷、血压血糖极值等危急症状，请立即拨打 120 或就近急诊就医。
          </li>
          <li>
            <strong>数据发送与隐私说明：</strong>
            您输入的内容提炼后会发送给第三方权威检索与大模型服务进行比对；服务端不持久保存输入内容，缓存仅保存在当前浏览器本地（24 小时）。
          </li>
          <li>
            <strong>个人信息保护：</strong>
            请勿在文本中输入姓名、身份证号、电话号码等敏感个人信息。
          </li>
        </ul>
      </div>

      <div class="modal-footer">
        <button type="button" class="btn-secondary" @click="$emit('cancel')">
          暂不核验
        </button>
        <button type="button" class="btn-confirm" @click="$emit('confirm')">
          我已阅读并知晓，开始核验
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  visible: boolean
}>()

defineEmits<{
  (e: 'confirm'): void
  (e: 'cancel'): void
}>()
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(2px);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.modal-card {
  background: #ffffff;
  max-width: 540px;
  width: 100%;
  border-radius: 18px;
  padding: 24px;
  box-shadow: 0 20px 40px -10px rgba(15, 23, 42, 0.2);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.modal-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.shield-badge-box {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: #eff6ff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-header h3 {
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
}

.modal-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.intro-line {
  font-size: 13px;
  color: #475569;
}

.consent-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 10px;
  border: 1px solid #f1f5f9;
}

.consent-list li {
  font-size: 12px;
  color: #334155;
  line-height: 1.5;
  position: relative;
  padding-left: 14px;
}

.consent-list li::before {
  content: '•';
  position: absolute;
  left: 2px;
  color: #2563eb;
  font-weight: 800;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 4px;
}

.btn-secondary {
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  border-radius: 999px;
  background: #f1f5f9;
}

.btn-confirm {
  padding: 8px 20px;
  font-size: 13px;
  font-weight: 700;
  color: #ffffff;
  border-radius: 999px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
}
</style>
