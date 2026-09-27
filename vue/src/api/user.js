import service, { request } from '../utils/request'

/** 账号与登录相关接口 */
export const authApi = {
  /** 注册（返回 { username, token }） */
  register(payload) {
    return request(service.post('/api/token/register', payload))
  },
  /** 登录（返回 { username, token }） */
  login(payload) {
    return request(service.post('/api/token/login', payload))
  },
  /** 校验当前 Token 是否有效 */
  checkToken() {
    return request(service.get('/api/token'))
  }
}

/** 个人资料相关接口 */
export const profileApi = {
  /** 个人资料（用户名 / 邮箱 / 注册时间等） */
  info() {
    return request(service.get('/api/profile'))
  },
  /** 用户名是否可用 */
  usernameAvailable(username) {
    return request(
      service.get('/api/profile/username-available', { params: { username } })
    )
  },
  /** 修改用户名 */
  updateUsername(payload) {
    return request(service.put('/api/profile/username', payload))
  },
  /** 修改密码 */
  updatePassword(payload) {
    return request(service.put('/api/profile/password', payload))
  },
  /** 修改邮箱 */
  updateEmail(payload) {
    return request(service.put('/api/profile/email', payload))
  }
}

/** 站内消息相关接口 */
export const notificationApi = {
  /** 分页列表 */
  page(pageNum = 1, pageSize = 10) {
    return request(service.get('/api/notifications', { params: { pageNum, pageSize } }))
  },
  /** 未读数 */
  unreadCount() {
    return request(service.get('/api/notifications/unread-count'))
  },
  /** 标记单条已读 */
  markRead(id) {
    return request(service.put(`/api/notifications/${id}/read`))
  },
  /** 全部已读 */
  markAllRead() {
    return request(service.put('/api/notifications/read-all'))
  }
}

/** 第三方登录 / 绑定相关接口 */
export const thirdPartyApi = {
  /** 已启用的第三方平台列表 */
  providers() {
    return request(service.get('/api/third-party/providers'))
  },
  /** 生成授权跳转地址（登录用） */
  authorizeUrl(provider) {
    return request(service.get(`/api/third-party/${provider}/authorize-url`))
  },
  /** 生成授权跳转地址（绑定用） */
  bindUrl(provider) {
    return request(service.get(`/api/third-party/${provider}/bind-url`))
  },
  /** 用一次性票据换取 JWT */
  exchange(ticket) {
    return request(service.post('/api/third-party/exchange', { ticket }))
  },
  /** 已绑定的第三方账号 */
  bindings() {
    return request(service.get('/api/third-party/bindings'))
  },
  /** 解绑 */
  unbind(provider) {
    return request(service.delete(`/api/third-party/bindings/${provider}`))
  }
}

/** 数据看板 */
export const statsApi = {
  dashboard() {
    return request(service.get('/api/stats/dashboard'))
  }
}
