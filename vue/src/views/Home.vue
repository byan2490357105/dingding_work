<template>
  <div>
    <el-menu :default-active="activeIndex" class="el-menu-demo" mode="horizontal">
      <el-menu-item index="1"><router-link to="/">主页</router-link></el-menu-item>
      <el-menu-item index="2" style="margin-left: auto">
        <router-link v-show="!isLogin" to="/login">登录</router-link>
        <el-dropdown @command="loginOut">
          <span style="color: #fff" v-show="isLogin">
            {{ username }}
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command>登出</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-menu-item>
    </el-menu>
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
      </div>
    </el-card>
  </div>
</template>

<script>
import { mapActions, mapGetters, mapState } from 'vuex'

export default {
  name: 'Home',
  data() {
    return {
      activeIndex: '1'
    }
  },
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
    ...mapActions(['userLoginOut']),
    // 登出
    loginOut() {
      this.userLoginOut()
      this.$router.replace('/login')
      this.$message.success('登出成功')
    },
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
.home-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}
</style>
