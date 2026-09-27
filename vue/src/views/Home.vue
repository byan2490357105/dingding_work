<template>
  <div class="home-page">
    <!-- 欢迎区 -->
    <section class="hero">
      <p class="hero-date">{{ todayText }}</p>
      <h1 class="hero-title">{{ greeting }}，{{ username || '朋友' }}</h1>
      <p class="hero-sub">今天想记点什么、要做点什么？从这里开始。</p>
    </section>

    <!-- 功能入口卡片 -->
    <section class="entry-grid">
      <div class="entry-card entry-notes" v-show="isLogin" @click="goNotes">
        <div class="entry-icon"><el-icon><EditPen /></el-icon></div>
        <div class="entry-body">
          <div class="entry-name">随手记</div>
          <div class="entry-desc">富文本笔记 · 标签置顶 · 词云 · 导出</div>
        </div>
        <el-icon class="entry-arrow"><ArrowRight /></el-icon>
      </div>

      <div class="entry-card entry-todo" v-show="isLogin" @click="goTodo">
        <div class="entry-icon"><el-icon><AlarmClock /></el-icon></div>
        <div class="entry-body">
          <div class="entry-name">待办任务</div>
          <div class="entry-desc">提醒 · 日历 · 时间轴 · AI 完成率预测</div>
        </div>
        <el-icon class="entry-arrow"><ArrowRight /></el-icon>
      </div>

      <div class="entry-card entry-dashboard" v-show="isLogin" @click="$router.push('/dashboard')">
        <div class="entry-icon"><el-icon><TrendCharts /></el-icon></div>
        <div class="entry-body">
          <div class="entry-name">数据看板</div>
          <div class="entry-desc">完成趋势 · 产出热力图 · 标签分布</div>
        </div>
        <el-icon class="entry-arrow"><ArrowRight /></el-icon>
      </div>

      <div class="entry-card entry-bindings" v-show="isLogin" @click="$router.push('/settings/profile?tab=bindings')">
        <div class="entry-icon"><el-icon><Connection /></el-icon></div>
        <div class="entry-body">
          <div class="entry-name">账号绑定</div>
          <div class="entry-desc">钉钉第三方登录 · 账号安全</div>
        </div>
        <el-icon class="entry-arrow"><ArrowRight /></el-icon>
      </div>
    </section>

    <!-- 未登录引导 -->
    <section class="login-guide" v-show="!isLogin">
      <el-button type="primary" size="large" @click="$router.push('/login')">立即登录</el-button>
      <el-button size="large" plain @click="$router.push('/register')">注册新账号</el-button>
    </section>
  </div>
</template>

<script>
import { mapGetters, mapState } from 'vuex'
import { EditPen, AlarmClock, TrendCharts, Connection, ArrowRight } from '@element-plus/icons-vue'
import { authApi } from '../api/user'
import { notifyError } from '../utils/notify'

export default {
  name: 'Home',
  components: { EditPen, AlarmClock, TrendCharts, Connection, ArrowRight },
  computed: {
    ...mapState(['username', 'token']),
    ...mapGetters(['isLogin']),
    // 今天日期的展示文案
    todayText() {
      const now = new Date()
      const week = ['日', '一', '二', '三', '四', '五', '六'][now.getDay()]
      return `${now.getFullYear()} 年 ${now.getMonth() + 1} 月 ${now.getDate()} 日 · 星期${week}`
    },
    // 按时段问候
    greeting() {
      const h = new Date().getHours()
      if (h < 6) return '夜深了'
      if (h < 12) return '早上好'
      if (h < 14) return '中午好'
      if (h < 18) return '下午好'
      return '晚上好'
    }
  },
  created() {
    // 页面加载时向后端校验 token，Token 失效时拦截器会自动登出并跳转登录页
    // 页面加载时向后端校验 token，Token 失效时拦截器会自动登出并跳转登录页
    authApi.checkToken().catch(err => notifyError(err, '登录状态校验失败'))
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
  max-width: 900px;
  margin: 0 auto;
  padding: 48px 20px;
}
.hero {
  text-align: center;
  margin-bottom: 36px;
}
.hero-date {
  margin: 0 0 10px;
  font-size: 13px;
  letter-spacing: 1px;
  color: var(--wb-text-muted);
}
.hero-title {
  margin: 0 0 10px;
  font-size: 30px;
  font-weight: 700;
  color: var(--wb-text-primary);
}
.hero-sub {
  margin: 0;
  font-size: 15px;
  color: var(--wb-text-secondary);
}
.entry-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
  gap: 16px;
}
.entry-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border: 1px solid var(--wb-border);
  border-radius: 14px;
  padding: 22px 24px;
  cursor: pointer;
  box-shadow: var(--wb-card-shadow);
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}
.entry-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--wb-card-shadow-hover);
}
.entry-card:hover .entry-arrow {
  transform: translateX(4px);
  color: var(--wb-primary);
}
.entry-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  color: #fff;
  flex-shrink: 0;
}
.entry-notes .entry-icon { background: linear-gradient(135deg, #4c84ff, #6ba0ff); }
.entry-todo .entry-icon { background: linear-gradient(135deg, #ff9f43, #ffc36e); }
.entry-dashboard .entry-icon { background: linear-gradient(135deg, #22c1a3, #5fd9c0); }
.entry-bindings .entry-icon { background: linear-gradient(135deg, #7b5bff, #a48bff); }
.entry-body {
  flex: 1;
  min-width: 0;
}
.entry-name {
  font-size: 17px;
  font-weight: 700;
  color: var(--wb-text-primary);
  margin-bottom: 4px;
}
.entry-desc {
  font-size: 13px;
  color: var(--wb-text-muted);
}
.entry-arrow {
  color: var(--wb-text-muted);
  font-size: 18px;
  transition: transform 0.22s ease, color 0.22s ease;
}
.login-guide {
  text-align: center;
  margin-top: 30px;
}
</style>
