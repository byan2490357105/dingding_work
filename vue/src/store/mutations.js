import * as types from './mutation-types'

// vuex 流程：view -> dispatch 触发 actions -> commit mutations -> state -> view
export default {
  // 登录成功后提交，data 为接口返回的 { token, name, ... }
  [types.LOGIN](state, data) {
    // token 和 username 同时存入 localStorage，保证刷新后登录状态不丢失
    state.token = data.token
    state.username = data.username
    localStorage.setItem('token', data.token)
    localStorage.setItem('username', data.username)
  },
  [types.LOGINOUT](state) {
    state.token = ''
    state.username = ''
    localStorage.removeItem('token')
    localStorage.removeItem('username')
  }
}
