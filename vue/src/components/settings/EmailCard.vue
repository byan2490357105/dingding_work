<template>
  <el-card class="setting-card">
    <template #header>
      <div class="card-header">
        <el-icon><Message /></el-icon>
        <span>邮箱设置</span>
      </div>
    </template>

    <el-form
      ref="emailForm"
      :model="emailForm"
      :rules="emailRules"
      label-width="100px"
      @submit.prevent
    >
      <el-form-item label="邮箱" prop="email">
        <el-input
          v-model="emailForm.email"
          placeholder="请输入邮箱，用于定时提醒推送（留空表示取消推送）"
          clearable
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="savingEmail" @click="submitEmail">保存邮箱</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script>
import { Message } from '@element-plus/icons-vue'
import { profileApi } from '../../api/user'

export default {
  name: 'EmailCard',
  components: { Message },
  data() {
    const validateEmail = (rule, value, callback) => {
      const v = (value || '').trim()
      if (!v) {
        callback()
      } else if (v.indexOf('@') === -1) {
        callback(new Error('邮箱格式不正确，需包含 @'))
      } else {
        callback()
      }
    }
    return {
      emailForm: { email: '' },
      emailRules: {
        email: [{ validator: validateEmail, trigger: 'blur' }]
      },
      savingEmail: false
    }
  },
  created() {
    this.loadEmail()
  },
  methods: {
    async loadEmail() {
      try {
        const data = await profileApi.info()
        if (data) {
          this.emailForm.email = data.email || ''
        }
      } catch (err) {
        // 加载失败不弹窗
      }
    },
    submitEmail() {
      this.$refs.emailForm.validate(async valid => {
        if (!valid) return
        this.savingEmail = true
        try {
          await profileApi.updateEmail({ email: this.emailForm.email.trim() })
          this.$message.success('邮箱保存成功')
        } catch (err) {
          this.$message.error(err.message || '保存失败')
        } finally {
          this.savingEmail = false
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
</style>
