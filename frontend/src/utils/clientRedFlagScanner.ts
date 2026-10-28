import type { RedFlagResult } from '../types/factcheck'

/**
 * 前端危急红线前置扫描工具（对应需求文档 US-02、3.3、4.1 节）
 *
 * 核心规则：
 * 1. “前端先扫，服务端收到请求后再独立扫一次。拦截级则停止，不发起任何检索与模型调用；
 *     用户点‘这只是文章里的内容’后降为提示级继续。”
 * 2. 生理指标阈值：收缩压 >=180 或舒张压 >=110 mmHg；空腹血糖 >=16.7 或 <=2.8 mmol/L；
 * 3. 急症症状：持续剧烈或压榨样胸痛、大汗淋漓、突发意识障碍、昏迷、言语不清、单侧肢体无力、
 *    严重呼吸困难、大咯血、突发爆炸样剧烈头痛等；
 * 4. 当前状态线索：第一人称或家人称谓 + 现时词/求助句式。
 */

export const HARD_RED_FLAG_MESSAGE =
  '检测到突发危急重症高危信号，请立即就近急诊就医或拨打 120 急救电话，切勿依赖网络自查！'

export const WARN_RED_FLAG_MESSAGE =
  '文中提到了可能危急的症状。如果您或家人此刻正有类似情况，请立即就医或拨打 120，不要依赖网络自查。'

/** 血压斜杠格式如 180/110 */
const BP_SLASH_REGEX = /(?<![\d/.])(\d{2,3})\s*\/\s*(\d{2,3})(?![\d/.])/g
/** 收缩压口语写法如 高压180、收缩压190 */
const BP_HIGH_REGEX = /(高压|收缩压|血压)\s*(?:[:：为达到有是飙升至到]|超过|高于|大于|>=|≥)*\s*(\d{2,3})(?!\d)/g
/** 舒张压口语写法如 低压110、舒张压115 */
const BP_LOW_REGEX = /(低压|舒张压)\s*(?:[:：为达到有是升至到]|超过|高于|大于|>=|≥)*\s*(\d{2,3})(?!\d)/g
/** 血糖口语写法如 血糖 2.5、空腹血糖 17.2 */
const GLUCOSE_REGEX =
  /(?:空腹|餐后|随机)?血糖\s*(?:[:：为达到有是降到升到]|超过|高于|大于|低于|小于|>=|<=|≥|≤)*\s*(\d{1,3}(?:\.\d+)?)(?!\s*(?:小时|分钟|h|H|min))/g

/** 危急症状词表 */
const CRITICAL_SYMPTOMS = [
  '剧烈胸痛',
  '持续胸痛',
  '压榨样胸痛',
  '胸口压榨',
  '胸痛大汗',
  '大汗淋漓',
  '突发意识障碍',
  '意识障碍',
  '昏迷',
  '神志不清',
  '不省人事',
  '叫不醒',
  '言语不清',
  '口齿不清',
  '无法说话',
  '嘴歪眼斜',
  '口角歪斜',
  '单侧肢体无力',
  '一侧手脚无力',
  '偏瘫',
  '肢体瘫痪',
  '半身不遂',
  '严重呼吸困难',
  '呼吸极度困难',
  '喘不上气',
  '紫绀',
  '大咯血',
  '剧烈咯血',
  '大口吐血',
  '呕血',
  '突发爆炸样剧烈头痛',
  '突发剧烈爆炸样头痛',
  '爆炸样头痛',
  '剧烈头痛欲裂',
  '急性心肌梗死',
  '心肌梗死',
  '急性心梗',
  '脑出血',
  '急性脑梗',
  '全身抽搐',
  '抽搐不止',
  '想自杀',
  '不想活了',
  '轻生'
]

/** 否定前缀正则 */
const NEGATION_PREFIX_REGEX = /(没有|没|无|不是|并非|未出现|否认|不曾|不会)[^，。；！？,.;!?]{0,4}$/

/** 第一人称或家人称谓 */
const PERSON_CLUES = [
  '我',
  '老伴',
  '爷爷',
  '奶奶',
  '外公',
  '外婆',
  '父亲',
  '母亲',
  '爸爸',
  '妈妈',
  '老公',
  '老婆',
  '孩子',
  '家里人'
]

/** 现时状态词 */
const TIME_CLUES = ['现在', '刚刚', '刚才', '突然', '正在', '此刻', '这会儿', '今天突然', '突然出现']

/** 求助句式 */
const HELP_CLUES = ['该怎么办', '怎么办', '要不要去医院', '要送医院吗', '需要去急诊吗', '求救', '救命', '救救我']

/** 极端紧急求救词 */
const URGENT_CLUES = ['救命', '救救我', '快救救']

/**
 * 在浏览器端执行前置危急红线扫描
 *
 * @param text              用户输入文本
 * @param ignoreHardRedFlag 用户是否已点击“这只是文章里的内容（继续核验）”
 */
export function scanRedFlagClient(text: string, ignoreHardRedFlag = false): RedFlagResult {
  if (!text || !text.trim()) {
    return { level: 'none' }
  }

  const bpHit = detectBloodPressure(text)
  const glucoseHit = detectGlucose(text)
  const symptomHit = detectSymptom(text)

  const category = bpHit ? 'bp' : glucoseHit ? 'glucose' : symptomHit ? 'symptom' : undefined
  if (!category) {
    return { level: 'none' }
  }

  const isCurrentState = hasCurrentStateClues(text)
  const level = isCurrentState && !ignoreHardRedFlag ? 'hard' : 'warn'
  return {
    level,
    category,
    message: level === 'hard' ? HARD_RED_FLAG_MESSAGE : WARN_RED_FLAG_MESSAGE
  }
}

function detectBloodPressure(text: string): boolean {
  BP_SLASH_REGEX.lastIndex = 0
  let match: RegExpExecArray | null
  while ((match = BP_SLASH_REGEX.exec(text)) !== null) {
    const sbp = parseInt(match[1], 10)
    const dbp = parseInt(match[2], 10)
    if (sbp > dbp && sbp <= 300 && dbp >= 30 && (sbp >= 180 || dbp >= 110)) {
      return true
    }
  }

  BP_HIGH_REGEX.lastIndex = 0
  while ((match = BP_HIGH_REGEX.exec(text)) !== null) {
    const val = parseInt(match[2], 10)
    if (val >= 180 && val <= 300) {
      return true
    }
  }

  BP_LOW_REGEX.lastIndex = 0
  while ((match = BP_LOW_REGEX.exec(text)) !== null) {
    const val = parseInt(match[2], 10)
    if (val >= 110 && val <= 200) {
      return true
    }
  }
  return false
}

function detectGlucose(text: string): boolean {
  GLUCOSE_REGEX.lastIndex = 0
  let match: RegExpExecArray | null
  while ((match = GLUCOSE_REGEX.exec(text)) !== null) {
    const raw = parseFloat(match[1])
    if (raw <= 0) continue
    const mmol = raw > 33.3 ? raw / 18.0 : raw
    if (mmol >= 16.7 || mmol <= 2.8) {
      return true
    }
  }
  return false
}

function detectSymptom(text: string): boolean {
  for (const symptom of CRITICAL_SYMPTOMS) {
    let idx = text.indexOf(symptom)
    while (idx >= 0) {
      const prefix = text.substring(Math.max(0, idx - 8), idx)
      if (!NEGATION_PREFIX_REGEX.test(prefix)) {
        return true
      }
      idx = text.indexOf(symptom, idx + symptom.length)
    }
  }
  return false
}

function hasCurrentStateClues(text: string): boolean {
  if (URGENT_CLUES.some((k) => text.includes(k))) {
    return true
  }
  const sentences = text.split(/[。！？!?\r\n]+/)
  for (const sentence of sentences) {
    if (!sentence.trim()) continue
    const hasPerson = PERSON_CLUES.some((p) => sentence.includes(p))
    const hasTimeOrHelp =
      TIME_CLUES.some((t) => sentence.includes(t)) || HELP_CLUES.some((h) => sentence.includes(h))
    if (hasPerson && hasTimeOrHelp) {
      return true
    }
  }
  if (text.length <= 80 && PERSON_CLUES.some((p) => text.includes(p))) {
    return TIME_CLUES.some((t) => text.includes(t)) || HELP_CLUES.some((h) => text.includes(h))
  }
  return false
}

