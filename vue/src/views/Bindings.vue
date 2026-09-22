<template>
  <div class="bindings-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>账号绑定</span>
          <el-button size="small" @click="loadBindings">刷新</el-button>
        </div>
      </template>

      <el-alert
        title="绑定第三方账号后，可以直接使用第三方平台登录，并且可以操作该平台的文档/表格。"
        type="info"
        :closable="false"
        show-icon
        class="tip"
      ></el-alert>

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
  </div>
</template>

<script>
export default {
  name: 'Bindings',
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
    // 从第三方绑定回调回来时给出提示
    if (this.$route.query.bind) {
      this.$message.success('绑定成功')
      this.$router.replace({ path: '/settings/bindings' })
    }
  },
  methods: {
    async loadProviders() {
      try {
        const res = await this.$http.get('/api/third-party/providers')
        if (res.data.code === 200) {
          this.providers = res.data.data || []
        }
      } catch (err) {
        // 忽略，页面仍可查看已有绑定
      }
    },
    async loadBindings() {
      this.loading = true
      try {
        const res = await this.$http.get('/api/third-party/bindings')
        if (res.data.code === 200) {
          this.bindings = res.data.data || []
        } else {
          this.$message.error(res.data.message || '加载绑定信息失败')
        }
      } catch (err) {
        this.handleError(err)
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
        const res = await this.$http.get(`/api/third-party/${provider}/bind-url`)
        if (res.data.code === 200 && res.data.data) {
          window.location.href = res.data.data
        } else {
          this.$message.error(res.data.message || '发起绑定失败')
        }
      } catch (err) {
        this.handleError(err)
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
            const res = await this.$http.delete(`/api/third-party/bindings/${provider}`)
            if (res.data.code === 200) {
              this.$message.success('解绑成功')
              this.loadBindings()
            } else {
              this.$message.error(res.data.message || '解绑失败')
            }
          } catch (err) {
            this.handleError(err)
          }
        })
        .catch(() => {})
    },
    handleError(err) {
      if (!err.response || err.response.status !== 401) {
        this.$message.error((err.response && err.response.data && err.response.data.message) || err.message)
      }
    }
  }
}
</script>

<style scoped>
.bindings-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
  text-align: left;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
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
