import { ElMessage } from 'element-plus'
import { isAuthError } from './request'

/**
 * 统一的接口错误提示。
 *
 * - 401 已由 utils/request.js 的响应拦截器统一清空登录态并跳转登录页，
 *   这里直接返回，避免重复弹窗。
 * - 优先展示后端返回的中文提示（GlobalExceptionHandler 已统一 message），
 *   后端未给出时退回到调用方传入的兜底文案。
 *
 * 用法：catch (err) { notifyError(err, '加载日历数据失败') }
 */
export function notifyError(err, fallback) {
  if (isAuthError(err)) return
  ElMessage.error((err && err.message) || fallback)
}
