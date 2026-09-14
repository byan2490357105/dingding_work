<template>
  <div class="callback-page">
    <el-card class="callback-card">
      <el-result
        :icon="resultIcon"
        :title="resultTitle"
        :sub-title="resultDetail"
      >
        <template #extra>
          <el-button v-if="finished" type="primary" @click="goHome">返回主页</el-button>
        </template>
      </el-result>
    </el-card>
  </div>
</template>

<script>
import { mapActions } from 'vuex'

export default {
  name: 'OAuthCallback',
  data() {
    return {
      resultIcon: 'info',
      resultTitle: '正在处理登录信息...',
      resultDetail: '请稍候',
      finished: false
    }
  },
  created() {
    this.handleCallback()
  },
  methods: {
    ...mapActions(['userLogin']),
    async handleCallback() {
      const query = this.$route.query
      if (query.error) {
        this.finish('error', '第三方登录失败', String(query.error))
        return
      }
      // 绑定成功回跳
      if (query.bind) {
        this.finish('success', '绑定成功', `已成功绑定${query.bind}账号`)
        setTimeout(() => this.$router.replace('/settings/bindings'), 1200)
        return
      }
      if (!query.ticket) {
        this.finish('error', '登录失败', '缺少登录票据，请重新登录')
        return
      }
      try {
        const res = await this.$http.post('/api/third-party/exchange', { ticket: query.ticket })
        if (res.data.code === 200 && res.data.data) {
          this.userLogin(res.data.data)
          this.finish('success', '登录成功', '正在进入工作台...')
          setTimeout(() => this.$router.replace('/'), 800)
        } else {
          this.finish('error', '登录失败', res.data.message || '票据校验失败')
        }
      } catch (err) {
        const message = (err.response && err.response.data && err.response.data.message) || err.message
        this.finish('error', '登录失败', message)
      }
    },
    finish(icon, title, detail) {
      this.resultIcon = icon
      this.resultTitle = title
      this.resultDetail = detail
      this.finished = true
      if (icon === 'error') {
        this.$message.error(detail)
      } else {
        this.$message.success(title)
      }
    },
    goHome() {
      this.$router.replace('/')
    }
  }
}
</script>

<style scoped>
.callback-page {
  display: flex;
  justify-content: center;
  padding: 60px 20px;
}
.callback-card {
  width: 520px;
}
</style>
