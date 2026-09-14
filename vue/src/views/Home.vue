<template>
  <div class="home-page">
    <el-card class="box-card">
      <p>Hello {{ username }}</p>
      <div class="home-actions">
        <el-button
          v-show="isLogin"
          type="primary"
          size="large"
          @click="goNotes"
        >
          随手记
        </el-button>
        <el-button
          v-show="isLogin"
          type="primary"
          size="large"
          @click="goTodo"
        >
          待办任务
        </el-button>
        <el-button
          v-show="isLogin"
          size="large"
          @click="$router.push('/settings/bindings')"
        >
          账号绑定
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script>
import { mapGetters, mapState } from 'vuex'

export default {
  name: 'Home',
  computed: {
    ...mapState(['username', 'token']),
    ...mapGetters(['isLogin'])
  },
  created() {
    // 页面加载时向后端校验 token，Token 失效时拦截器会自动登出并跳转登录页
    this.$http.get('/api/token')
      .catch(err => {
        // 401 已由响应拦截器统一处理，这里避免重复弹出错误
        if (!err.response || err.response.status !== 401) {
          this.$message.error(`${err.message}`)
        }
      })
  },
  methods: {
    // 跳转随手记页面
    goNotes() {
      this.$router.push('/notes')
    },
    // 跳转待办任务页面
    goTodo() {
      this.$router.push('/todo')
    }
  }
}
</script>

<style scoped>
.home-page {
  padding: 30px 20px;
}
.box-card {
  max-width: 720px;
  margin: 0 auto;
}
.home-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}
</style>
