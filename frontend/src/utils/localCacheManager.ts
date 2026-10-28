import type { FactCheckTaskResponse, LocalCacheEntry } from '../types/factcheck'

/**
 * 浏览器本地 24 小时缓存与首次知情须知状态管理器（对应需求文档 US-01、US-09、3.1、3.2、4.9、4.10 节）
 *
 * 规则：
 * 1. 同一内容 24 小时内重复提交命中本机缓存时，直接展示并标注“本机缓存结果”，提供“重新核验”；
 * 2. 存在“待重试（RETRY_NEEDED）”条目时，整次结果绝不写入缓存（allowClientCache === false）；
 * 3. 记录首次使用知情确认状态（仅需一次知情确认）。
 */

const CACHE_STORAGE_KEY = 'familydoc_sentinel_cache_v1'
const CONSENT_STORAGE_KEY = 'familydoc_sentinel_consent_confirmed_v1'
const CACHE_TTL_MS = 24 * 60 * 60 * 1000 // 24 小时
const MAX_CACHE_ITEMS = 30

/**
 * 对输入文本做去除首尾空白、合并连续空行预处理（对应需求 3.1 节）
 */
export function preprocessInputText(raw: string): string {
  if (!raw) return ''
  return raw
    .replace(/[\u200B-\u200F\u202A-\u202E\u2060\uFEFF\u00AD]/g, '')
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .replace(/(\n[ \t\u3000]*){2,}/g, '\n')
    .trim()
}

/**
 * 计算规范化文本指纹（忽略标点与空白差异，用于本机缓存匹配）
 */
export function computeClientFingerprint(text: string): string {
  const normalized = preprocessInputText(text)
    .normalize('NFKC')
    .toLowerCase()
    .replace(/[\p{P}\p{S}\s]+/gu, '')
  const source = normalized || text.trim()

  // FNV-1a 双 32 位哈希组合生成确定性十六进制指纹
  let h1 = 0x811c9dc5
  let h2 = 0x01000193
  for (let i = 0; i < source.length; i++) {
    const ch = source.charCodeAt(i)
    h1 ^= ch
    h1 = Math.imul(h1, 0x01000193)
    h2 ^= ch * (i + 1)
    h2 = Math.imul(h2, 0x811c9dc5)
  }
  return (h1 >>> 0).toString(16).padStart(8, '0') + (h2 >>> 0).toString(16).padStart(8, '0')
}

/**
 * 读取全部未过期的本地缓存记录（按保存时间倒序）
 */
export function loadValidCacheEntries(): LocalCacheEntry[] {
  try {
    const raw = localStorage.getItem(CACHE_STORAGE_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw) as LocalCacheEntry[]
    if (!Array.isArray(parsed)) return []

    const now = Date.now()
    const valid = parsed.filter(
      (item) => item && item.savedAt && now - item.savedAt < CACHE_TTL_MS && item.response
    )
    if (valid.length !== parsed.length) {
      localStorage.setItem(CACHE_STORAGE_KEY, JSON.stringify(valid))
    }
    return valid
  } catch {
    return []
  }
}

/**
 * 根据输入文本查找 24 小时内的本机缓存结果
 */
export function findLocalCachedResult(text: string): LocalCacheEntry | null {
  const fp = computeClientFingerprint(text)
  const entries = loadValidCacheEntries()
  return entries.find((item) => item.fingerprint === fp) || null
}

/**
 * 将已完成的完整核验结果写入本机 24 小时缓存
 * 严格遵循需求 4.9 节：存在“待重试”条目（allowClientCache === false）时不写入缓存
 */
export function saveLocalCachedResult(rawText: string, response: FactCheckTaskResponse): void {
  if (!response || response.status !== 'COMPLETED' || !response.allowClientCache) {
    return
  }
  const hasRetryItem = (response.claims || []).some(
    (c) => c.verdictCode === 'RETRY_NEEDED' || c.retryable
  )
  if (hasRetryItem) {
    return
  }

  try {
    const fp = computeClientFingerprint(rawText)
    const entries = loadValidCacheEntries().filter((e) => e.fingerprint !== fp)
    const newEntry: LocalCacheEntry = {
      fingerprint: fp,
      rawText: preprocessInputText(rawText),
      savedAt: Date.now(),
      response: JSON.parse(JSON.stringify(response))
    }
    entries.unshift(newEntry)
    const trimmed = entries.slice(0, MAX_CACHE_ITEMS)
    localStorage.setItem(CACHE_STORAGE_KEY, JSON.stringify(trimmed))
  } catch {
    // 忽略隐私模式或配额超限异常
  }
}

/**
 * 清空本机全部缓存记录
 */
export function clearAllLocalCache(): void {
  try {
    localStorage.removeItem(CACHE_STORAGE_KEY)
  } catch {
    // ignore
  }
}

/**
 * 检查用户是否已确认过首次使用知情须知（需求 US-01）
 */
export function hasConfirmedConsent(): boolean {
  try {
    return localStorage.getItem(CONSENT_STORAGE_KEY) === 'true'
  } catch {
    return false
  }
}

/**
 * 记录用户已确认首次使用知情须知
 */
export function markConsentConfirmed(): void {
  try {
    localStorage.setItem(CONSENT_STORAGE_KEY, 'true')
  } catch {
    // ignore
  }
}

