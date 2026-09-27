/**
 * 统一的时间处理工具。
 *
 * 背景：后端 LocalDateTime 在不同序列化配置下可能是
 *   1) ISO 字符串 "2026-09-27T14:02:40"
 *   2) 数组 [2026, 9, 27, 14, 2, 40]（Jackson 关闭 WRITE_DATES_AS_TIMESTAMPS 前的旧格式）
 * 之前项目里 NoteCard / Notes / Todo / NotificationCard 各写了一份 formatTime，
 * 这里统一实现，避免多处拷贝导致行为不一致。
 */

const pad = n => String(n).padStart(2, '0')

/**
 * 将后端时间解析为容器对象，兼容 ISO 字符串与数组两种格式。
 * @returns {{y:number,m:number,d:number,hh:number,mm:number,ss:number}|null}
 */
export function parseParts(value) {
  if (value === null || value === undefined || value === '') return null
  if (Array.isArray(value)) {
    const [y, m, d, hh = 0, mm = 0, ss = 0] = value
    if (!y || !m || !d) return null
    return { y, m, d, hh, mm, ss }
  }
  const text = String(value)
  const m = text.match(
    /^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2})(?::(\d{2}))?)?/
  )
  if (!m) return null
  return {
    y: Number(m[1]),
    m: Number(m[2]),
    d: Number(m[3]),
    hh: Number(m[4] || 0),
    mm: Number(m[5] || 0),
    ss: Number(m[6] || 0)
  }
}

/** 解析为 Date 对象（按本地时区，避免 new Date('2026-09-27 14:02') 在部分浏览器返回 Invalid Date） */
export function parseLocal(value) {
  const p = parseParts(value)
  if (!p) return new Date(NaN)
  return new Date(p.y, p.m - 1, p.d, p.hh, p.mm, p.ss)
}

/**
 * 日期时间格式化，默认 "yyyy-MM-dd HH:mm"。
 * 支持格式串：yyyy MM dd HH mm ss
 */
export function formatDateTime(value, pattern = 'yyyy-MM-dd HH:mm') {
  const p = parseParts(value)
  if (!p) return ''
  return pattern
    .replace('yyyy', String(p.y))
    .replace('MM', pad(p.m))
    .replace('dd', pad(p.d))
    .replace('HH', pad(p.hh))
    .replace('mm', pad(p.mm))
    .replace('ss', pad(p.ss))
}

/** 只到分钟，等价于原 formatTime / formatHistoryTime */
export const formatMinute = value => formatDateTime(value, 'yyyy-MM-dd HH:mm')

/** 只要日期 yyyy-MM-dd */
export const formatDate = value => formatDateTime(value, 'yyyy-MM-dd')

/** 只要 "yyyy-MM" */
export const formatMonth = value => formatDateTime(value, 'yyyy-MM')

/**
 * 把 Date 对象格式化为 yyyy-MM-dd（日历单元格 key 用）
 */
export function toDateKey(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/** 取时间字符串的日期部分（yyyy-MM-dd），后端给的 ISO 串可直接切 */
export function datePart(value) {
  const parts = parseParts(value)
  if (!parts) return ''
  return `${parts.y}-${pad(parts.m)}-${pad(parts.d)}`
}

/** 取时间字符串的 "HH:mm" 部分 */
export function timePart(value) {
  const parts = parseParts(value)
  if (!parts) return ''
  return `${pad(parts.hh)}:${pad(parts.mm)}`
}

/** 日期的友好描述：今天 / 明天 / 昨天 / MM-DD */
export function friendlyDate(value) {
  const parts = parseParts(value)
  if (!parts) return ''
  const key = `${parts.y}-${pad(parts.m)}-${pad(parts.d)}`
  const today = toDateKey(new Date())
  const d = new Date(parts.y, parts.m - 1, parts.d)
  const diff = Math.round(
    (d.getTime() - new Date(today + 'T00:00').getTime()) / 86400000
  )
  if (diff === 0) return '今天'
  if (diff === 1) return '明天'
  if (diff === -1) return '昨天'
  return `${pad(parts.m)}-${pad(parts.d)}`
}

/** 把日期加减天数后返回 yyyy-MM-dd */
export function shiftDay(dateKey, days) {
  const [y, m, d] = String(dateKey).split('-').map(Number)
  const dt = new Date(y, m - 1, d + days)
  return toDateKey(dt)
}
