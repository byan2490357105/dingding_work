<template>
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
</template>

<script>
import { Lock } from '@element-plus/icons-vue'

export default {
  name: 'PasswordCard',
  components: { Lock },
  data() {
    const validateConfirm = (rule, value, callback) => {
      if (value !== this.passwordForm.newPassword) {
        callback(new Error('两次输入的新密码不一致'))
      } else {
        callback()
      }
    }
    return {
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
  methods: {
    submitPassword() {
      this.$refs.passwordForm.validate(async valid => {
        if (!valid) return
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
</style>
