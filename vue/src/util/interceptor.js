import axios from 'axios'
import router from '../router'
import store from '../store'
import * as types from '../store/mutation-types'

const service = axios.create({
  timeout: 10000,
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

// 响应拦截：后端返回 401 时清空登录态并跳转登录页
service.interceptors.response.use(
  response => response,
  err => {
    if (err.response && err.response.status === 401) {
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
    return Promise.reject(err)
  }
)

export default service
