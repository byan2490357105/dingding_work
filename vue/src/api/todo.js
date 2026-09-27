import service, { request } from '../utils/request'

/** 待办相关接口 */
export const todoApi = {
  /**
   * 分页查询
   * @param {{pageNum:number,pageSize:number,status?:string,keyword?:string,excludeLabel?:string[]}} params
   */
  page(params) {
    return request(service.get('/api/todo/page', { params }))
  },
  /** 轻量计数 { pending, done, total, trash } */
  counts() {
    return request(service.get('/api/todo/counts'))
  },
  /**
   * 轻量区间查询（日历 / 时间轴视图）
   * @param {{start?:string,end?:string,overlap?:boolean,completed?:boolean,limit?:number}} params
   *   start/end 同为 yyyy-MM-dd；同时省略表示「不限时间」
   */
  range(params = {}) {
    return request(service.get('/api/todo/range', { params }))
  },
  /** 全量标签统计 [{ label, count }] */
  labelStats() {
    return request(service.get('/api/todo/labels'))
  },
  /** 词云词频 [{ name, value }] */
  wordCloud(limit = 50) {
    return request(service.get('/api/todo/wordcloud', { params: { limit } }))
  },
  /** 详情 */
  detail(id) {
    return request(service.get(`/api/todo/${id}`))
  },
  /** 设置完成状态 */
  setCompleted(id, completed) {
    return request(
      service.put(`/api/todo/${id}/complete`, null, { params: { completed } })
    )
  },
  /** 新建 / 修改 */
  save(payload) {
    return payload.id
      ? request(service.put(`/api/todo/${payload.id}`, payload))
      : request(service.post('/api/todo', payload))
  },
  /** 移入回收站 */
  remove(id) {
    return request(service.delete(`/api/todo/${id}`))
  },
  /** 回收站列表 */
  trash() {
    return request(service.get('/api/todo/trash'))
  },
  /** 恢复 */
  restore(id) {
    return request(service.put(`/api/todo/restore/${id}`))
  },
  /** 彻底删除 */
  permanentRemove(id) {
    return request(service.delete(`/api/todo/permanent/${id}`))
  },
  /** 导出 CSV，返回原始 blob 响应 */
  exportFile() {
    return service.get('/api/todo/export/csv', { responseType: 'blob' })
  }
}
