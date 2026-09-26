<template>
  <div class="settings-layout">
    <!-- 侧边栏导航 -->
    <aside class="settings-sidebar">
      <el-menu :default-active="activeTab" @select="onMenuSelect">
        <el-menu-item index="username">
          <el-icon><User /></el-icon>
          <span>个人资料</span>
        </el-menu-item>
        <el-menu-item index="password">
          <el-icon><Lock /></el-icon>
          <span>修改密码</span>
        </el-menu-item>
        <el-menu-item index="email">
          <el-icon><Message /></el-icon>
          <span>邮箱设置</span>
        </el-menu-item>
        <el-menu-item index="bindings">
          <el-icon><Connection /></el-icon>
          <span>第三方账号</span>
        </el-menu-item>
        <el-menu-item index="notifications">
          <el-icon><Bell /></el-icon>
          <span>消息提示</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <!-- 内容区：动态切换卡片组件 -->
    <section class="settings-content">
      <component :is="currentComponent" />
    </section>
  </div>
</template>

<script>
import { User, Lock, Connection, Message, Bell } from '@element-plus/icons-vue'
import UsernameCard from '../components/settings/UsernameCard.vue'
import PasswordCard from '../components/settings/PasswordCard.vue'
import EmailCard from '../components/settings/EmailCard.vue'
import BindingsCard from '../components/settings/BindingsCard.vue'
import NotificationCard from '../components/settings/NotificationCard.vue'

const VALID_TABS = ['username', 'password', 'email', 'bindings', 'notifications']

export default {
  name: 'Profile',
  components: { User, Lock, Connection, Message, Bell },
  data() {
    return {
      activeTab: 'username'
    }
  },
  computed: {
    currentComponent() {
      const map = {
        username: UsernameCard,
        password: PasswordCard,
        email: EmailCard,
        bindings: BindingsCard,
        notifications: NotificationCard
      }
      return map[this.activeTab]
    }
  },
  created() {
    // 从路由 query 初始化激活的 tab（支持 URL 分享/刷新回显）
    const tab = this.$route.query.tab
    if (tab && VALID_TABS.includes(tab)) {
      this.activeTab = tab
    }
  },
  methods: {
    onMenuSelect(index) {
      this.activeTab = index
      // 同步到路由 query（replace 避免历史栈堆积）
      this.$router.replace({ path: '/settings/profile', query: { tab: index } })
    }
  }
}
</script>

<style scoped>
.settings-layout {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px 20px;
  display: flex;
  gap: 24px;
  text-align: left;
}
.settings-sidebar {
  width: 200px;
  flex-shrink: 0;
}
.settings-content {
  flex: 1;
  min-width: 0;
}
</style>
