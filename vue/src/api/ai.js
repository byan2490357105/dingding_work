import service, { request } from '../utils/request'

/** AI 分析相关接口 */
const AI_BASE = '/api/ai'

export const aiApi = {
  /** 笔记月报 */
  noteMonthly() {
    return request(service.get(`${AI_BASE}/report/notes`))
  },
  /** 笔记周报 */
  noteWeekly() {
    return request(service.get(`${AI_BASE}/report/notes/weekly`))
  },
  /** 待办月报 */
  todoMonthly() {
    return request(service.get(`${AI_BASE}/report/todo`))
  },
  /** 待办周报 */
  todoWeekly() {
    return request(service.get(`${AI_BASE}/report/todo/weekly`))
  },
  /** 历史报告列表（type: note-monthly / note-weekly / todo-monthly / todo-weekly） */
  history(type, limit = 12) {
    return request(service.get(`${AI_BASE}/report/history`, { params: { type, limit } }))
  },
  /** 新建任务完成成功率预测 */
  predict(payload) {
    return request(service.post(`${AI_BASE}/predict`, payload))
  }
}
