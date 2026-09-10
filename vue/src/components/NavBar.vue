<template>
  <header class="navbar">
    <div class="navbar-inner">
      <!-- 品牌区 -->
      <div class="brand" @click="go('/')">
        <el-icon class="brand-icon"><Notebook /></el-icon>
        <span class="brand-text">随手工作台</span>
      </div>

      <!-- 导航菜单 -->
      <el-menu
        class="nav-menu"
        mode="horizontal"
        :default-active="activeMenu"
        :ellipsis="false"
        @select="go"
      >
        <el-menu-item index="/">主页</el-menu-item>
        <el-menu-item index="/notes">随手记</el-menu-item>
        <el-menu-item index="/todo">待办任务</el-menu-item>
      </el-menu>

      <!-- 右侧用户区：个人中心式头像下拉 -->
      <div class="nav-right">
        <template v-if="isLogin">
          <el-dropdown trigger="click" @command="onCommand">
            <div class="user-box">
              <el-avatar :size="32" class="user-avatar">{{ avatarText }}</el-avatar>
              <span class="username">{{ username }}</span>
              <el-icon class="arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled class="info-item">
                  <el-icon><User /></el-icon>&nbsp;{{ username }}
                </el-dropdown-item>
                <el-dropdown-item divided command="logout" class="logout-item">
                  <el-icon><SwitchButton /></el-icon>&nbsp;退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <el-button v-else type="primary" size="small" @click="go('/login')">登录</el-button>
      </div>
    </div>
  </header>
</template>

<script>
import { mapGetters, mapState, mapActions } from 'vuex'
import { Notebook, ArrowDown, User, SwitchButton } from '@element-plus/icons-vue'

export default {
  name: 'NavBar',
  components: { Notebook, ArrowDown, User, SwitchButton },
  computed: {
    ...mapState(['username']),
    ...mapGetters(['isLogin']),
    // 当前路由对应的菜单高亮项
    activeMenu() {
      const path = this.$route.path
      if (path.startsWith('/notes')) return '/notes'
      if (path.startsWith('/todo')) return '/todo'
      return '/'
    },
    // 头像取用户名首字，未取名时兜底显示“我”
    avatarText() {
      const name = (this.username || '').trim()
      return name ? name.charAt(0).toUpperCase() : '我'
    }
  },
  methods: {
    ...mapActions(['userLoginOut']),
    go(path) {
      if (this.$route.path !== path) {
        this.$router.push(path)
      }
    },
    // 下拉指令：退出登录 —— 独立控件，任何页面都可用
    onCommand(command) {
      if (command === 'logout') {
        this.$confirm('确定退出登录吗？', '提示', {
          confirmButtonText: '退出',
          cancelButtonText: '取消',
          type: 'warning'
        })
          .then(() => {
            this.userLoginOut()
            this.$message.success('已退出登录')
            this.$router.replace('/login')
          })
          .catch(() => {})
      }
    }
  }
}
</script>

<style scoped>
.navbar {
  width: 100%;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  position: sticky;
  top: 0;
  z-index: 100;
}
.navbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  height: 60px;
  padding: 0 20px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  margin-right: 24px;
}
.brand-icon {
  font-size: 24px;
  color: var(--el-color-primary);
}
.brand-text {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
  white-space: nowrap;
}
.nav-menu {
  flex: 1;
  border-bottom: none !important;
}
.nav-right {
  display: flex;
  align-items: center;
}
.user-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 20px;
  cursor: pointer;
  outline: none;
  transition: background-color 0.2s;
}
.user-box:hover {
  background: var(--el-fill-color-light);
}
.user-avatar {
  background: var(--el-color-primary);
  color: #fff;
  font-weight: 600;
  flex-shrink: 0;
}
.username {
  font-size: 14px;
  color: #303133;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.arrow {
  color: #909399;
  font-size: 12px;
}
.logout-item {
  color: var(--el-color-danger);
}
.info-item {
  cursor: default !important;
  color: #303133;
  font-weight: 600;
}
</style>
