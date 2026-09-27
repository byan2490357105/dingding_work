<template>
  <div class="auth-page">
    <div class="auth-panel">
      <!-- 左侧品牌区 -->
      <div class="auth-brand">
        <div class="brand-badge"><el-icon><Notebook /></el-icon></div>
        <h1 class="brand-name">随手工作台</h1>
        <p class="brand-slogan">随手记 · 待办 · AI 分析，一个工作台全搞定</p>
        <ul class="brand-points">
          <li><el-icon><EditPen /></el-icon>富文本随手记，标签 / 置顶 / 词云</li>
          <li><el-icon><AlarmClock /></el-icon>待办提醒 + 日历 + 时间轴视图</li>
          <li><el-icon><MagicStick /></el-icon>DeepSeek 驱动的周报 / 月报分析</li>
        </ul>
      </div>

      <!-- 右侧表单区 -->
      <div class="auth-form-side">
        <h2 class="auth-title">欢迎回来</h2>
        <p class="auth-subtitle">登录你的账号，继续记录生活</p>

        <el-form
          label-position="top"
          :model="formLogin"
          :rules="rules"
          ref="formLogin"
          class="auth-form">
          <el-form-item label="账号" prop="username">
            <el-input v-model="formLogin.username" size="large" placeholder="请输入账号">
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="formLogin.password"
              type="password"
              show-password
              size="large"
              placeholder="请输入密码"
              @keyup.enter="login"
            >
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" class="auth-submit" @click="login">登 录</el-button>
          </el-form-item>
          <el-form-item>
            <router-link to="/register" class="auth-switch-link">
              还没有账号？<span>立即注册 →</span>
            </router-link>
          </el-form-item>
        </el-form>

        <el-divider class="auth-divider">第三方登录</el-divider>
        <div class="third-party-login">
          <el-button
            v-for="provider in providers"
            :key="provider.provider"
            type="primary"
            plain
            size="large"
            @click="thirdPartyLogin(provider.provider)"
          >
            {{ provider.displayName }}登录（扫码 / 手机号）
          </el-button>
          <el-text v-if="providers.length === 0" type="info">
            暂无可用的第三方登录，请先在后台配置应用密钥
          </el-text>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapActions } from 'vuex'
import { Notebook, EditPen, AlarmClock, MagicStick, User, Lock } from '@element-plus/icons-vue'
import { authApi, thirdPartyApi } from '../api/user'

export default {
  name: 'Login',
  components: { Notebook, EditPen, AlarmClock, MagicStick, User, Lock },
  data() {
    let checkUserName = (rule, value, cb) => {
      if (!value) {
        return cb(new Error('账号不能为空!'))
      } else {
        cb() // 将判断传递给后面
      }
    }
    let checkPassword = (rule, value, cb) => {
      if (!value) {
        return cb(new Error('密码不能为空!'))
      } else {
        cb()
      }
    }
    return {
      formLogin: {
        username: '',
        password: ''
      },
      rules: {
        username: [
          { validator: checkUserName, trigger: 'blur' }
        ],
        password: [
          { validator: checkPassword, trigger: 'blur' }
        ]
      },
      providers: []
    }
  },
  created() {
    this.loadProviders()
  },
  methods: {
    ...mapActions(['userLogin']),
    // 向登录接口发起请求
    login() {
      const formData = {
        username: this.formLogin.username,
        password: this.formLogin.password
      }
      // 表单验证
      this.$refs['formLogin'].validate(async valid => {
        if (!valid) {
          this.$message.error('表单验证失败!')
          return false
        }
        try {
          // 后端已启用「HTTP 状态码 = 业务 code」的错误契约，
          // 失败会直接进入 catch，err.message 即后端中文提示
          const data = await authApi.login(formData)
          this.userLogin(data)
          this.$message.success('登录成功')
          // 登录成功跳转回来源页面或首页
          this.$router.replace(this.$route.query.redirect || '/')
        } catch (err) {
          this.$message.error(err.message || '登录失败')
        }
      })
    },
    // 加载已启用的第三方登录平台
    async loadProviders() {
      try {
        this.providers = (await thirdPartyApi.providers()) || []
      } catch (err) {
        // 加载失败时保持空列表，页面会给出提示
      }
    },
    // 跳转第三方授权页（钉钉登录页同时提供扫码登录与手机号登录）
    async thirdPartyLogin(provider) {
      try {
        const url = await thirdPartyApi.authorizeUrl(provider)
        if (url) {
          window.location.href = url
        } else {
          this.$message.error('发起第三方登录失败')
        }
      } catch (err) {
        this.$message.error(err.message || '发起第三方登录失败')
      }
    }
  }
}
</script>

<style scoped>
.auth-page {
  min-height: calc(100vh - 60px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}
.auth-panel {
  display: flex;
  width: 920px;
  max-width: 100%;
  min-height: 560px;
  border-radius: 20px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 24px 64px rgba(38, 58, 110, 0.16);
}
.auth-brand {
  flex: 1 1 46%;
  background: var(--wb-gradient-brand);
  color: #fff;
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  overflow: hidden;
}
.auth-brand::before,
.auth-brand::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
}
.auth-brand::before {
  width: 260px;
  height: 260px;
  top: -80px;
  right: -60px;
}
.auth-brand::after {
  width: 180px;
  height: 180px;
  bottom: -60px;
  left: -40px;
}
.brand-badge {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  margin-bottom: 20px;
  position: relative;
  z-index: 1;
}
.brand-name {
  margin: 0 0 10px;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 1px;
  position: relative;
  z-index: 1;
}
.brand-slogan {
  margin: 0 0 26px;
  font-size: 14px;
  opacity: 0.9;
  line-height: 1.6;
  position: relative;
  z-index: 1;
}
.brand-points {
  margin: 0;
  padding: 0;
  list-style: none;
  position: relative;
  z-index: 1;
}
.brand-points li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 14px;
  opacity: 0.94;
}
.auth-form-side {
  flex: 1 1 54%;
  padding: 48px 44px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.auth-title {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 700;
  color: var(--wb-text-primary);
}
.auth-subtitle {
  margin: 0 0 24px;
  font-size: 14px;
  color: var(--wb-text-muted);
}
.auth-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--wb-text-secondary);
}
.auth-submit {
  width: 100%;
  margin-top: 4px;
  letter-spacing: 6px;
}
.auth-switch-link {
  font-size: 14px;
  color: var(--wb-text-muted);
}
.auth-switch-link span {
  color: var(--wb-primary-deep);
  font-weight: 600;
}
.auth-divider {
  margin: 8px 0 16px;
}
.third-party-login {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}
@media (max-width: 760px) {
  .auth-brand {
    display: none;
  }
  .auth-panel {
    min-height: auto;
  }
}
</style>
