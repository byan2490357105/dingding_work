import { request } from '../utils/request'
import service from '../utils/request'

/** 随手记相关接口 */
export const notesApi = {
  /** 分页查询（keyword / excludeTag 可空） */
  page(params) {
    return request(service.get('/api/notes/page', { params }))
  },
  /** 全量标签统计 [{ tag, count }] */
  tagStats() {
    return request(service.get('/api/notes/tags'))
  },
  /** 词云词频 [{ name, value }] */
  wordCloud(limit = 50) {
    return request(service.get('/api/notes/wordcloud', { params: { limit } }))
  },
  /** 详情 */
  detail(id) {
    return request(service.get(`/api/notes/${id}`))
  },
  /** 新建 / 修改（有 id 走 PUT） */
  save(payload) {
    return payload.id
      ? request(service.put('/api/notes', payload))
      : request(service.post('/api/notes', payload))
  },
  /** 置顶 / 取消置顶 */
  togglePin(id) {
    return request(service.put(`/api/notes/pin/${id}`))
  },
  /** 移入回收站 */
  remove(id) {
    return request(service.delete(`/api/notes/${id}`))
  },
  /** 回收站列表 */
  trash() {
    return request(service.get('/api/notes/trash'))
  },
  /** 从回收站恢复 */
  restore(id) {
    return request(service.put(`/api/notes/restore/${id}`))
  },
  /** 彻底删除 */
  permanentRemove(id) {
    return request(service.delete(`/api/notes/permanent/${id}`))
  },
  /** 导出（CSV / Word），返回原始 blob 响应 */
  exportFile(type) {
    return service.get(`/api/notes/export/${type}`, { responseType: 'blob' })
  }
}
