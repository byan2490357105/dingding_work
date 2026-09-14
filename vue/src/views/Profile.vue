<template>
  <div class="profile-page">
    <!-- 修改用户名 -->
    <el-card class="setting-card">
      <template #header>
        <div class="card-header">
          <el-icon><User /></el-icon>
          <span>修改用户名</span>
        </div>
      </template>

      <el-form
        ref="usernameForm"
        :model="usernameForm"
        :rules="usernameRules"
        label-width="100px"
        @submit.prevent
      >
        <el-form-item label="当前用户名">
          <el-input :model-value="currentUsername" disabled></el-input>
        </el-form-item>
        <el-form-item label="新用户名" prop="newUsername">
          <el-input
            v-model="usernameForm.newUsername"
            placeholder="请输入新用户名"
            clearable
            @input="onUsernameInput"
          >
            <template #append>
              <el-button :loading="checking" @click="checkUsername">检查是否可用</el-button>
            </template>
          </el-input>
          <div v-if="usernameTip" class="field-tip" :class="usernameAvailable ? 'ok' : 'err'">
            {{ usernameTip }}
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingUsername" @click="submitUsername">保存用户名</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 修改密码 -->
    <el-card class="setting-card">
      <template #header>
        <div class="card-header">
          <el-icon><Lock /></el-icon>
          <span>修改密码</span>
        </div>
      </template>

      <el-form
        ref="passwordForm"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="100px"
        @submit.prevent
      >
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入旧密码"></el-input>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="至少6位"></el-input>
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="再次输入新密码"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingPassword" @click="submitPassword">保存新密码</el-button>
          <el-button @click="resetPasswordForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 第三方账号 -->
    <el-card class="setting-card">
      <template #header>
        <div class="card-header">
          <el-icon><Connection /></el-icon>
          <span>第三方账号</span>
        </div>
      </template>
      <el-button type="primary" plain @click="$router.push('/settings/bindings')">管理钉钉等第三方绑定</el-button>
    </el-card>
  </div>
</template>

<script>
import { mapActions, mapState } from 'vuex'
import { User, Lock, Connection } from '@element-plus/icons-vue'

export default {
  name: 'Profile',
  components: { User, Lock, Connection },
  data() {
    // 确认新密码校验
    const validateConfirm = (rule, value, callback) => {
      if (value !== this.passwordForm.newPassword) {
        callback(new Error('两次输入的新密码不一致'))
      } else {
        callback()
      }
    }
    // 新用户名自定义校验：必须通过重名检查
    const validateUnique = (rule, value, callback) => {
      if (!value || !value.trim()) {
        callback(new Error('用户名不能为空'))
      } else if (this.usernameChecked && !this.usernameAvailable) {
        callback(new Error('该用户名已被使用，请换一个'))
      } else {
        callback()
      }
    }
    return {
      usernameForm: {
        newUsername: ''
      },
      usernameRules: {
        newUsername: [{ validator: validateUnique, trigger: 'blur' }]
      },
      checking: false,
      savingUsername: false,
      usernameChecked: false,
      usernameAvailable: false,
      usernameTip: '',
      checkTimer: null,

      passwordForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      passwordRules: {
        oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, message: '新密码长度不能少于6位', trigger: 'blur' }
        ],
        confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }]
      },
      savingPassword: false
    }
  },
  computed: {
    ...mapState(['username']),
    currentUsername() {
      return this.username
    }
  },
  methods: {
    ...mapActions(['userLogin']),
    // 输入时防抖自动查重（500ms）
    onUsernameInput() {
      this.usernameChecked = false
      this.usernameTip = ''
      if (this.checkTimer) {
        clearTimeout(this.checkTimer)
      }
      const value = this.usernameForm.newUsername.trim()
      if (!value) {
        return
      }
      // 和当前用户名一样，直接判定可用，不发请求
      if (value === this.currentUsername) {
        this.usernameChecked = true
        this.usernameAvailable = true
        this.usernameTip = '新用户名与当前相同，无需修改'
        return
      }
      this.checkTimer = setTimeout(() => {
        this.checkUsername()
      }, 500)
    },
    // 调后端查重接口
    async checkUsername() {
      const value = this.usernameForm.newUsername.trim()
      if (!value) {
        this.usernameTip = '用户名不能为空'
        this.usernameAvailable = false
        this.usernameChecked = true
        return
      }
      if (value === this.currentUsername) {
        this.usernameChecked = true
        this.usernameAvailable = true
        this.usernameTip = '新用户名与当前相同，无需修改'
        return
      }
      this.checking = true
      try {
        const res = await this.$http.get('/api/profile/username-available', {
          params: { username: value }
        })
        this.usernameChecked = true
        if (res.data.code === 200) {
          this.usernameAvailable = res.data.data === true
          this.usernameTip = this.usernameAvailable ? '该用户名可以使用' : '该用户名已被使用，请换一个'
        } else {
          this.usernameAvailable = false
          this.usernameTip = res.data.message || '检查失败'
        }
      } catch (err) {
        this.usernameChecked = true
        this.usernameAvailable = false
        this.usernameTip = '检查失败，请稍后重试'
      } finally {
        this.checking = false
      }
    },
    // 提交修改用户名
    submitUsername() {
      this.$refs.usernameForm.validate(async valid => {
        if (!valid) {
          return
        }
        // 提交前再查一次，防止检查后名字被抢注
        await this.checkUsername()
        if (!this.usernameAvailable) {
          this.$message.error('该用户名已被使用，请换一个')
          return
        }
        if (this.usernameForm.newUsername.trim() === this.currentUsername) {
          this.$message.info('新用户名与当前相同，无需修改')
          return
        }
        this.savingUsername = true
        try {
          const res = await this.$http.put('/api/profile/username', {
            newUsername: this.usernameForm.newUsername.trim()
          })
          if (res.data.code === 200) {
            // 后端返回新 Token（旧 Token 里的用户名已过期），刷新本地登录态
            this.userLogin(res.data.data)
            this.$message.success('用户名修改成功')
            this.usernameForm.newUsername = ''
            this.usernameTip = ''
            this.usernameChecked = false
          } else {
            this.$message.error(res.data.message || '修改失败')
          }
        } catch (err) {
          this.$message.error((err.response && err.response.data && err.response.data.message) || '修改失败')
        } finally {
          this.savingUsername = false
        }
      })
    },
    // 提交修改密码
    submitPassword() {
      this.$refs.passwordForm.validate(async valid => {
        if (!valid) {
          return
        }
        this.savingPassword = true
        try {
          const res = await this.$http.put('/api/profile/password', {
            oldPassword: this.passwordForm.oldPassword,
            newPassword: this.passwordForm.newPassword
          })
          if (res.data.code === 200) {
            this.$message.success('密码修改成功')
            this.resetPasswordForm()
          } else {
            this.$message.error(res.data.message || '修改失败')
          }
        } catch (err) {
          this.$message.error((err.response && err.response.data && err.response.data.message) || '修改失败')
        } finally {
          this.savingPassword = false
        }
      })
    },
    resetPasswordForm() {
      this.$refs.passwordForm.resetFields()
    }
  }
}
</script>

<style scoped>
.profile-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px 20px;
  text-align: left;
}
.setting-card {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
}
.field-tip {
  font-size: 12px;
  margin-top: 4px;
  line-height: 1.4;
}
.field-tip.ok {
  color: var(--el-color-success);
}
.field-tip.err {
  color: var(--el-color-danger);
}
</style>
