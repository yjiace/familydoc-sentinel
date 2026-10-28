/**
 * 家医智核·流言粉碎 前后端数据契约与类型定义
 * 严格对应后端 cn.smallyoung.familydocsentinel.dto 与 enums 包下的模型定义
 */

/**
 * 后端统一响应包装结构 SysResult<T>
 */
export interface SysResult<T> {
  code: number
  message: string
  data: T
}

/**
 * 创建核验任务请求参数 FactCheckRequest
 */
export interface FactCheckRequest {
  /** 待核验的健康文本（上限 2000 字符） */
  text: string
  /** 用户是否点击了“这只是文章里的内容（继续核验）”，将 HARD 红线降级为 WARN */
  ignoreHardRedFlag?: boolean
  /** 是否跳过缓存强制重新核验 */
  skipCache?: boolean
}

/**
 * 危急红线级别：none(未命中) | warn(提示级) | hard(拦截级)
 */
export type RedFlagLevel = 'none' | 'warn' | 'hard'

/**
 * 危急红线分类：bp(血压) | glucose(血糖) | symptom(危急急症症状)
 */
export type RedFlagCategory = 'bp' | 'glucose' | 'symptom'

/**
 * 危急红线扫描结果 RedFlagResult
 */
export interface RedFlagResult {
  level: RedFlagLevel
  category?: RedFlagCategory
  message?: string
}

/**
 * 用户意图识别结果 IntentAnalysisResult
 */
export interface IntentAnalysisResult {
  valid: boolean
  category: 'FACT_CHECKABLE' | 'GREETING' | 'NON_HEALTH' | 'VAGUE' | 'MALICIOUS' | string
  reason: string
  guideMessage: string
}

/**
 * 印章视觉元数据 StampDto（对应需求 US-05 与 4.8 节）
 */
export interface StampDto {
  /** 结论编码：SUPPORTED | MISLEADING | CONDITIONAL | UNVERIFIED | ASK_DOCTOR */
  verdictCode: string
  /** 口语大字：有依据 | 有误导 | 看情况 | 没查实 | 问医生 */
  colloquialText: string
  /** 严谨小字：证据相符 | 与依据矛盾 | 条件受限 | 无法核实 | 请咨询医生 */
  rigorousText: string
  /** 建议色值 Hex */
  colorHex: string
  /** 边框形态编码 */
  borderShapeCode: string
  /** 边框形态中文说明 */
  borderShapeDesc: string
  /** 印章旁固定标注：“AI 辅助核验·非官方结论” */
  sideNote: string
}

/**
 * 核验出处与证据条目 EvidenceItemDto（对应需求 US-03、4.5、4.6 节）
 */
export interface EvidenceItemDto {
  /** 序号（1~4） */
  index: number
  /** 来源机构名称 */
  sourceOrg: string
  /** 规范化主域名 */
  domain: string
  /** 文章标题 */
  title: string
  /** 发布日期（缺失时为“日期未标注”） */
  publishDate: string
  /** 来源摘要（800 字以内按句截取，界面统一称“来源摘要”） */
  sourceSummary: string
  /** 原文安全链接 */
  url: string
  /** 来源分级编码：TIER_1 | TIER_2 */
  tierCode: 'TIER_1' | 'TIER_2' | string
  /** 来源分级中文名称：“一级来源” | “二级来源” */
  tierLabel: string
  /** 是否为主流媒体健康频道（需标注“媒体报道”） */
  mediaReport: boolean
  /** 来源类型标签 */
  sourceTag: string
  /** 相关性得分 */
  relevanceScore: number
  /** 综合排序分 */
  rankScore: number
  /** 立场编码：SUPPORT | REFUTE | CONDITIONAL | INSUFFICIENT | IRRELEVANT */
  stanceCode: 'SUPPORT' | 'REFUTE' | 'CONDITIONAL' | 'INSUFFICIENT' | 'IRRELEVANT' | string
  /** 立场中文名称：支持 | 否定 | 条件成立 | 缺乏证据 | 未涉及 */
  stanceLabel: string
  /** 逐字摘录引文（<=60 字） */
  quote: string
  /** 是否通过四项硬校验 */
  citationValid: boolean
  /** 附加标注（如“未据此下结论”或“口径不同”） */
  note?: string
}

/**
 * 单条可核验论断及其核验结果与实时检索状态 ClaimItemDto
 */
export interface ClaimItemDto {
  /** 论断唯一 ID（如 c-1） */
  claimId: string
  /** 原文出现顺序 */
  originalOrder: number
  /** 原子论断陈述文本 */
  claimText: string
  /** 分类编码：DRUG | DISEASE | LIFESTYLE */
  categoryCode: 'DRUG' | 'DISEASE' | 'LIFESTYLE' | string
  /** 分类中文名称 */
  categoryLabel: string
  /** 风险等级编码：HIGH | MEDIUM | LOW */
  riskLevelCode: 'HIGH' | 'MEDIUM' | 'LOW' | string
  /** 风险等级中文名称 */
  riskLevelLabel: string
  /** 是否为个体用药咨询 */
  personalMedication: boolean
  /** 中性检索词 */
  neutralQuery: string
  /** 实时检索/核验子阶段编码 */
  verifyStageCode:
    | 'PENDING'
    | 'PRIMARY_SEARCHING'
    | 'UPGRADE_SEARCHING'
    | 'EVIDENCE_ADMITTING'
    | 'STANCE_ANNOTATING'
    | 'ADJUDICATING'
    | 'COMPLETED'
    | 'RETRY_NEEDED'
    | string
  /** 实时检索/核验子阶段中文描述 */
  verifyStageText: string
  /** 已执行检索次数 */
  searchCount: number
  /** 裁决结论编码：SUPPORTED | MISLEADING | CONDITIONAL | UNVERIFIED | ASK_DOCTOR | RETRY_NEEDED */
  verdictCode?:
    | 'SUPPORTED'
    | 'MISLEADING'
    | 'CONDITIONAL'
    | 'UNVERIFIED'
    | 'ASK_DOCTOR'
    | 'RETRY_NEEDED'
    | string
  /** 印章口语大字 */
  colloquialVerdict?: string
  /** 印章严谨小字 */
  rigorousVerdict?: string
  /** 印章视觉元数据（待重试时为 null） */
  stamp?: StampDto | null
  /** 置信度编码：HIGH | MEDIUM | NONE */
  confidenceCode?: 'HIGH' | 'MEDIUM' | 'NONE' | string
  /** 置信度中文标签 */
  confidenceLabel?: string
  /** 结论说明文字（<=120 字） */
  explanation?: string
  /** 没查实五类细分原因编码 */
  unverifiedCauseCode?: string
  /** 没查实原因标签 */
  unverifiedCauseLabel?: string
  /** 没查实面向用户文案 */
  unverifiedCauseMessage?: string
  /** 没查实固定明示文案：“没查实不代表是真的，也不代表是假的” */
  unverifiedDisclaimer?: string
  /** 药物类论断固定安全提示 */
  drugSafetyNotice?: string
  /** 特殊附注（如“另有来源称研究不足”或“口径不同”） */
  extraNote?: string
  /** 主展示出处证据列表 */
  evidences: EvidenceItemDto[]
  /** 冲突或不同口径的另一方出处证据列表 */
  opposingEvidences: EvidenceItemDto[]
  /** 是否可单独点击重试 */
  retryable: boolean
}

/**
 * 未纳入核验的内容清单项 UnverifiedItemDto（对应需求 US-08 与 4.2 节）
 */
export interface UnverifiedItemDto {
  /** 原文片段 */
  segmentText: string
  /** 未核验原因编码 */
  reasonCode: string
  /** 未核验原因中文说明 */
  reasonDescription: string
}

/**
 * 任务整体状态类型
 */
export type TaskStatusType = 'PROCESSING' | 'COMPLETED' | 'BLOCKED' | 'CANCELLED' | 'FAILED'

/**
 * 核验任务全量响应 FactCheckTaskResponse
 */
export interface FactCheckTaskResponse {
  taskId: string
  status: TaskStatusType | string
  statusDescription: string
  stage: string
  stageDescription: string
  progressPercent: number
  completedClaimCount: number
  totalClaimCount: number
  queryHash: string
  cacheHit: boolean
  allowClientCache: boolean
  redFlag?: RedFlagResult | null
  intentResult?: IntentAnalysisResult | null
  articleSummary?: string
  noticeMessage?: string
  claims: ClaimItemDto[]
  unverifiedItems: UnverifiedItemDto[]
  disclaimer: string
  privacyStatement: string
  updatedAt: string
}

/**
 * 浏览器本地缓存条目数据结构
 */
export interface LocalCacheEntry {
  /** 规范化全文指纹 */
  fingerprint: string
  /** 原始输入文本 */
  rawText: string
  /** 缓存写入时间戳（毫秒） */
  savedAt: number
  /** 完整的核验任务结果快照 */
  response: FactCheckTaskResponse
}

