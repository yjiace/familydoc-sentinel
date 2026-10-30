/**
 * 前端接口基础路径配置
 * - 本地开发 (.env.development): VITE_API_BASE_URL 为空，使用相对路径 /api/... 走 Vite 本地代理 (http://localhost:8080)
 * - 生产构建 (.env.production): VITE_API_BASE_URL=https://doc-api.smallyoung.cn，自动拼接完整跨域地址
 */
const rawBaseUrl = (import.meta.env.VITE_API_BASE_URL || '').trim()

export const API_ORIGIN = rawBaseUrl.replace(/\/+$/, '')

export function buildApiUrl(path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${API_ORIGIN}${normalizedPath}`
}

