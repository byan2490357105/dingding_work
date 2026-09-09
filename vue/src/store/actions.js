import * as types from './mutation-types'

// actions 接收 context，可直接解构出 commit
// 组件中通过 store.dispatch('userLogin', data) 触发
export default {
  userLogin({ commit }, data) {
    commit(types.LOGIN, data)
  },
  userLoginOut({ commit }) {
    commit(types.LOGINOUT)
  }
}
