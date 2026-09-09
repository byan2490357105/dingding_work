import { createStore } from 'vuex'
import mutations from './mutations'
import actions from './actions'

const state = {
  // 刷新页面后仍保留登录状态
  token: localStorage.getItem('token') || '',
  username: localStorage.getItem('username') || ''
}

const getters = {
  isLogin: state => !!state.token
}

export default createStore({
  state,
  getters,
  mutations,
  actions
})
