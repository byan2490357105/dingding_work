/**
 * 文件下载工具：把后端返回的 blob 响应落盘为本地文件。
 * 文件名优先取响应头 Content-Disposition 中的 filename*=UTF-8''xxx。
 */

/** 从响应头解析文件名，解析不到时用兜底名 */
export function filenameFromResponse(headers, fallback) {
  const disposition = (headers && headers['content-disposition']) || ''
  const match = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (match) {
    try {
      return decodeURIComponent(match[1])
    } catch (e) {
      return fallback
    }
  }
  return fallback
}

/**
 * 把 blob 响应保存为文件
 * @param {Promise} promise axios 请求（responseType: 'blob'）
 * @param {string} fallbackName 兜底文件名
 */
export async function downloadBlob(promise, fallbackName) {
  const res = await promise
  const filename = filenameFromResponse(res.headers, fallbackName)
  const url = URL.createObjectURL(res.data)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  return filename
}
