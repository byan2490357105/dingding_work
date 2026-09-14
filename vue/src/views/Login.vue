<template>
  <el-card class="box-card">
    <el-row justify="center">
      <el-col :span="12">
        <el-form
          label-position="left"
          label-width="80px"
          :model="formLogin"
          :rules="rules"
          ref="formLogin">
          <el-form-item label="账号" prop="username">
            <el-input v-model="formLogin.username"></el-input>
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="formLogin.password" type="password" show-password></el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="login">登录</el-button>
            <el-button @click="resetForm">取消</el-button>
          </el-form-item>
          <el-form-item>
            <router-link to="/register">
              <el-button>没有账号，立即注册</el-button>
            </router-link>
          </el-form-item>
        </el-form>
        <el-divider>第三方登录</el-divider>
        <div class="third-party-login">
          <el-button
            v-for="provider in providers"
            :key="provider.provider"
            type="primary"
            plain
            @click="thirdPartyLogin(provider.provider)"
          >
            {{ provider.displayName }}登录（扫码 / 手机号）
          </el-button>
          <el-text v-if="providers.length === 0" type="info">
            暂无可用的第三方登录，请先在后台配置应用密钥
          </el-text>
        </div>
      </el-col>
    </el-row>
  </el-card>
</template>

<script>
import { mapActions } from 'vuex'

export default {
  name: 'Login',
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
      let formData = {
        username: this.formLogin.username,
        password: this.formLogin.password
      }
      // 表单验证
      this.$refs['formLogin'].validate((valid) => {
        if (valid) {
          // 通过验证之后才请求登录接口
          this.$http.post('/api/token/login', formData)
            .then(res => {
              if (res.data.code === 200) {
                // res.data.data 为后端返回的 { username, token }
                this.userLogin(res.data.data)
                this.$message.success(res.data.message || '登录成功')
                // 登录成功跳转回来源页面或首页
                this.$router.replace(this.$route.query.redirect || '/')
              } else {
                this.$message.error(res.data.message || '登录失败')
                return false
              }
            })
            .catch(err => {
              this.$message.error(`${err.message}`)
            })
        } else {
          this.$message.error('表单验证失败!')
          return false
        }
      })
    },
    // 表单重置
    resetForm() {
      this.$refs['formLogin'].resetFields()
    },
    // 加载已启用的第三方登录平台
    loadProviders() {
      this.$http.get('/api/third-party/providers')
        .then(res => {
          if (res.data.code === 200) {
            this.providers = res.data.data || []
          }
        })
        .catch(() => {})
    },
    // 跳转第三方授权页（钉钉登录页同时提供扫码登录与手机号登录）
    thirdPartyLogin(provider) {
      this.$http.get(`/api/third-party/${provider}/authorize-url`)
        .then(res => {
          if (res.data.code === 200 && res.data.data) {
            window.location.href = res.data.data
          } else {
            this.$message.error(res.data.message || '发起第三方登录失败')
          }
        })
        .catch(err => {
          this.$message.error((err.response && err.response.data && err.response.data.message) || err.message)
        })
    }
  }
}
</script>

<style scoped>
.third-party-login {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}
</style>
