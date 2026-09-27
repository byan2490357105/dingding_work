<template>
  <div class="auth-page">
    <div class="auth-panel">
      <!-- 左侧品牌区 -->
      <div class="auth-brand">
        <div class="brand-badge"><el-icon><Notebook /></el-icon></div>
        <h1 class="brand-name">随手工作台</h1>
        <p class="brand-slogan">注册一个账号，开始你的高效记录之旅</p>
        <ul class="brand-points">
          <li><el-icon><EditPen /></el-icon>随手记 + 待办一站式管理</li>
          <li><el-icon><Calendar /></el-icon>日历 / 时间轴多视图浏览</li>
          <li><el-icon><MagicStick /></el-icon>AI 帮你总结与预测任务</li>
        </ul>
      </div>

      <!-- 右侧表单区 -->
      <div class="auth-form-side">
        <h2 class="auth-title">创建账号</h2>
        <p class="auth-subtitle">只需一步，立即开始使用</p>

        <el-form
          label-position="top"
          :model="formRegister"
          :rules="rules"
          ref="formRegister"
          class="auth-form">
          <el-form-item label="账号" prop="username">
            <el-input v-model="formRegister.username" size="large" placeholder="请输入账号">
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="formRegister.password" type="password" show-password size="large" placeholder="请输入密码">
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item label="确认密码" prop="checkPassword">
            <el-input v-model="formRegister.checkPassword" type="password" show-password size="large" placeholder="请再次输入密码" @keyup.enter="addUser">
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" class="auth-submit" @click="addUser">立即注册</el-button>
          </el-form-item>
          <el-form-item>
            <router-link to="/login" class="auth-switch-link">
              已有账号？<span>返回登录 →</span>
            </router-link>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script>
import { mapActions } from 'vuex'
import { Notebook, EditPen, Calendar, MagicStick, User, Lock } from '@element-plus/icons-vue'
import { authApi } from '../api/user'

export default {
  name: 'Register',
  components: { Notebook, EditPen, Calendar, MagicStick, User, Lock },
  data() {
    let checkUserName = (rule, value, cb) => {
      if (!value) {
        return cb(new Error('账户不能为空!'))
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
    let checkPasswordAgain = (rule, value, cb) => {
      if (!value) {
        return cb(new Error('再次输入密码不能为空!'))
      } else if (value !== this.formRegister.password) {
        return cb(new Error('两次输入密码不一致!'))
      } else {
        cb()
      }
    }
    return {
      formRegister: {
        username: '',
        password: '',
        checkPassword: ''
      },
      rules: {
        username: [
          { validator: checkUserName, trigger: 'blur' }
        ],
        password: [
          { validator: checkPassword, trigger: 'blur' }
        ],
        checkPassword: [
          { validator: checkPasswordAgain, trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    ...mapActions(['userLogin']),
    // 注册用户
    addUser() {
      const formData = {
        username: this.formRegister.username,
        password: this.formRegister.password
      }
      // 表单验证
      this.$refs['formRegister'].validate(async valid => {
        if (!valid) {
          this.$message.error('表单验证失败!')
          return false
        }
        try {
          // 注册成功即签发 Token，直接登录进入首页
          const data = await authApi.register(formData)
          this.userLogin(data)
          this.$message.success('注册成功')
          this.$router.replace('/')
        } catch (err) {
          this.$message.error(err.message || '注册失败')
        }
      })
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
}
.auth-switch-link {
  font-size: 14px;
  color: var(--wb-text-muted);
}
.auth-switch-link span {
  color: var(--wb-primary-deep);
  font-weight: 600;
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
