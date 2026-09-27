import axios from 'axios'
import router from '../router'
import store from '../store'
import * as types from '../store/mutation-types'

/**
 * 统一 HTTP 客户端。
 *
 * 错误契约（与后端 GlobalExceptionHandler 对齐）：
 *   - 业务成功：HTTP 200 + body { code: 200, message, data }
 *   - 业务/系统失败：HTTP 状态码 == body.code（400/401/403/404/409/500/502/503）
 *   - 401 统一视为「会话失效」，自动清空登录态并跳登录页
 *
 * 为了兼容历史写法，实例仍然导出给 $http 使用；
 * 新代码建议走 src/api/*（内部用 request() 已解包，直接拿到 data）。
 */
const service = axios.create({
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  },
  // 数组参数序列化为 key=a&key=b（Spring @RequestParam List 可直接绑定），
  // axios 默认会序列化成 key[]=a，后端收不到
  paramsSerializer: {
    serialize(params) {
      const parts = []
      Object.keys(params).forEach(key => {
        const val = params[key]
        if (val === null || val === undefined || val === '') return
        if (Array.isArray(val)) {
          val.forEach(v => {
            if (v !== null && v !== undefined && v !== '') {
              parts.push(encodeURIComponent(key) + '=' + encodeURIComponent(v))
            }
          })
        } else {
          parts.push(encodeURIComponent(key) + '=' + encodeURIComponent(val))
        }
      })
      return parts.join('&')
    }
  }
})

// 请求拦截：localStorage 存在 token 时自动携带 Authorization
service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  err => Promise.reject(err)
)

// 响应拦截：解包后端错误结构 + 401 统一登出
service.interceptors.response.use(
  response => response,
  err => {
    const response = err.response
    if (response) {
      // 后端异常统一为 Result 结构，把 message 提升到 err.message，
      // 这样页面上 `this.$message.error(err.message)` 就能直接展示中文提示
      const body = response.data
      if (body && typeof body === 'object' && body.message) {
        err.message = body.message
        err.code = body.code
      } else if (response.status === 500) {
        err.message = '服务器内部错误'
      }
      if (response.status === 401) {
        store.commit(types.LOGINOUT)
        if (router.currentRoute.value.path !== '/login') {
          router.replace({
            path: '/login',
            query: {
              redirect: router.currentRoute.value.fullPath
            }
          })
        }
      }
    } else if (err.code === 'ECONNABORTED') {
      err.message = '请求超时，请检查网络后重试'
    }
    return Promise.reject(err)
  }
)

/**
 * 业务封装：直接返回后端 body.data，失败抛出 Error（message 已本地化）。
 * 用法：
 *   const page = await request(service.get('/api/notes/page', { params }))
 *   const blob = await service.get('/api/notes/export/csv', { responseType: 'blob' })  // 文件流仍需原始响应
 */
export async function request(promise) {
  const res = await promise
  const body = res.data
  // 非 JSON（如文件流）直接返回原始响应
  if (body === null || body === undefined || typeof body !== 'object' || !('code' in body)) {
    return res
  }
  if (body.code === 200) {
    return body.data
  }
  const error = new Error(body.message || '请求失败')
  error.code = body.code
  throw error
}

/** 页面里的通用错误提示：401 已由拦截器统一处理，不重复弹窗 */
export function isAuthError(err) {
  return !!(err && err.response && err.response.status === 401)
}

export default service
