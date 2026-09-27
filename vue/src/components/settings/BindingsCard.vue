<template>
  <el-card class="setting-card">
    <template #header>
      <div class="card-header">
        <span>第三方账号</span>
        <el-button size="small" @click="loadBindings">刷新</el-button>
      </div>
    </template>

    <el-alert
      title="绑定第三方账号后，可直接使用该账号快捷登录随手工作台。"
      type="info"
      :closable="false"
      show-icon
      class="tip"
    >
      <template #default>
        当前仅提供「第三方快捷登录」能力：授权时只获取你的昵称与手机号用于账号匹配，
        不会读取、同步或修改第三方平台的任何文档、表格与联系人数据。可随时在本页解绑。
      </template>
    </el-alert>

    <div class="bind-actions">
      <el-button
        v-for="provider in providers"
        :key="provider.provider"
        type="primary"
        :disabled="isBound(provider.provider)"
        @click="bind(provider.provider)"
      >
        {{ isBound(provider.provider) ? `已绑定${provider.displayName}` : `绑定${provider.displayName}` }}
      </el-button>
      <el-text v-if="providers.length === 0" type="info">暂无可绑定的第三方平台</el-text>
    </div>

    <el-table :data="bindings" v-loading="loading" style="width: 100%">
      <el-table-column label="平台" width="120">
        <template #default="{ row }">{{ displayName(row.provider) }}</template>
      </el-table-column>
      <el-table-column prop="nickname" label="第三方昵称" show-overflow-tooltip></el-table-column>
      <el-table-column prop="mobile" label="手机号" width="150"></el-table-column>
      <el-table-column prop="bindTime" label="绑定时间" width="180"></el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="danger" @click="unbind(row.provider)">解绑</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="还没有绑定任何第三方账号" :image-size="80"></el-empty>
      </template>
    </el-table>
  </el-card>
</template>

<script>
import { thirdPartyApi } from '../../api/user'
import { notifyError } from '../../utils/notify'

export default {
  name: 'BindingsCard',
  data() {
    return {
      providers: [],
      bindings: [],
      loading: false
    }
  },
  created() {
    this.loadProviders()
    this.loadBindings()
    // OAuth 回跳成功提示
    if (this.$route.query.bind) {
      this.$message.success('绑定成功')
      this.$router.replace({ path: '/settings/profile', query: { tab: 'bindings' } })
    }
  },
  methods: {
    async loadProviders() {
      try {
        this.providers = (await thirdPartyApi.providers()) || []
      } catch (err) {
        // 忽略
      }
    },
    async loadBindings() {
      this.loading = true
      try {
        this.bindings = (await thirdPartyApi.bindings()) || []
      } catch (err) {
        notifyError(err, '加载绑定信息失败')
      } finally {
        this.loading = false
      }
    },
    isBound(provider) {
      return this.bindings.some(item => item.provider === provider)
    },
    displayName(provider) {
      const target = this.providers.find(item => item.provider === provider)
      return target ? target.displayName : provider
    },
    async bind(provider) {
      try {
        const url = await thirdPartyApi.bindUrl(provider)
        if (url) {
          window.location.href = url
        } else {
          this.$message.error('发起绑定失败')
        }
      } catch (err) {
        notifyError(err, '发起绑定失败')
      }
    },
    unbind(provider) {
      this.$confirm(`确定解绑${this.displayName(provider)}账号吗？`, '解绑确认', {
        type: 'warning',
        confirmButtonText: '解绑',
        cancelButtonText: '取消'
      })
        .then(async () => {
          try {
            await thirdPartyApi.unbind(provider)
            this.$message.success('解绑成功')
            this.loadBindings()
          } catch (err) {
            notifyError(err, '解绑失败')
          }
        })
        .catch(() => {})
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
  justify-content: space-between;
  font-weight: 600;
  font-size: 15px;
}
.tip {
  margin-bottom: 16px;
}
.bind-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
</style>
