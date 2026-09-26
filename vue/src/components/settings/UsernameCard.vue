<template>
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
</template>

<script>
import { mapActions, mapState } from 'vuex'
import { User } from '@element-plus/icons-vue'

export default {
  name: 'UsernameCard',
  components: { User },
  data() {
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
      usernameForm: { newUsername: '' },
      usernameRules: {
        newUsername: [{ validator: validateUnique, trigger: 'blur' }]
      },
      checking: false,
      savingUsername: false,
      usernameChecked: false,
      usernameAvailable: false,
      usernameTip: '',
      checkTimer: null
    }
  },
  computed: {
    ...mapState(['username']),
    currentUsername() {
      return this.username
    }
  },
  beforeUnmount() {
    // 清理防抖定时器，避免切换 tab 后回调操作已卸载组件
    if (this.checkTimer) {
      clearTimeout(this.checkTimer)
    }
  },
  methods: {
    ...mapActions(['userLogin']),
    onUsernameInput() {
      this.usernameChecked = false
      this.usernameTip = ''
      if (this.checkTimer) {
        clearTimeout(this.checkTimer)
      }
      const value = this.usernameForm.newUsername.trim()
      if (!value) return
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
    submitUsername() {
      this.$refs.usernameForm.validate(async valid => {
        if (!valid) return
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
    }
  }
}
</script>

<style scoped>
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
