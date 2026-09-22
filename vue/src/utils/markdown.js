/**
 * 轻量 Markdown 转 HTML，仅处理月报用到的语法：
 * 标题（## / ###）、加粗（**text**）、有序/无序列表、段落、换行。
 * 不做复杂嵌套，足够展示 AI 月报。
 */
export function renderMarkdown(md) {
  if (!md) return ''
  // 转义 HTML
  let html = String(md)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  // 加粗 **xxx**
  html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')

  const lines = html.split(/\r?\n/)
  const blocks = []
  let listBuffer = [] // { type: 'ul'|'ol', items: [] }

  const flushList = () => {
    if (listBuffer.length) {
      const first = listBuffer[0]
      const itemsHtml = listBuffer.map(it => `<li>${it.text}</li>`).join('')
      blocks.push(`<${first.type}>${itemsHtml}</${first.type}>`)
      listBuffer = []
    }
  }

  for (let line of lines) {
    const trimmed = line.trim()

    // 标题
    const h3 = trimmed.match(/^###\s+(.*)$/)
    const h2 = trimmed.match(/^##\s+(.*)$/)
    if (h3) {
      flushList()
      blocks.push(`<h3>${h3[1]}</h3>`)
      continue
    }
    if (h2) {
      flushList()
      blocks.push(`<h2>${h2[1]}</h2>`)
      continue
    }

    // 无序列表
    const ul = trimmed.match(/^[-*]\s+(.*)$/)
    if (ul) {
      if (listBuffer.length && listBuffer[0].type !== 'ul') flushList()
      listBuffer.push({ type: 'ul', text: ul[1] })
      continue
    }
    // 有序列表
    const ol = trimmed.match(/^\d+\.\s+(.*)$/)
    if (ol) {
      if (listBuffer.length && listBuffer[0].type !== 'ol') flushList()
      listBuffer.push({ type: 'ol', text: ol[1] })
      continue
    }

    flushList()
    if (trimmed) {
      blocks.push(`<p>${trimmed}</p>`)
    }
  }
  flushList()

  return blocks.join('\n')
}
