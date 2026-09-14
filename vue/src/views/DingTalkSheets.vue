<template>
  <div class="sheet-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>钉钉表格</span>
          <el-button size="small" @click="$router.push('/settings/bindings')">返回账号绑定</el-button>
        </div>
      </template>

      <el-alert
        title="读取/写入的是当前登录用户钉钉账号下有权限的表格。workbookId 是钉钉表格文件 id，range 形如 Sheet1!A1:D10。"
        type="info"
        :closable="false"
        show-icon
        class="tip"
      ></el-alert>

      <el-form label-width="100px">
        <el-form-item label="workbookId">
          <el-input v-model.trim="workbookId" placeholder="请输入钉钉表格 id"></el-input>
        </el-form-item>
        <el-form-item label="range">
          <el-input v-model.trim="range" placeholder="例如 Sheet1!A1:D10"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="listSheets">读取工作表</el-button>
          <el-button :loading="loading" @click="getRange">读取区域</el-button>
        </el-form-item>
        <el-form-item label="写入数据">
          <el-input
            v-model="valuesText"
            type="textarea"
            :rows="5"
            placeholder='二维数组 JSON，例如 [["姓名","分数"],["张三",90]]'
          ></el-input>
          <el-button class="write-btn" type="warning" :loading="loading" @click="updateRange">写入区域</el-button>
        </el-form-item>
      </el-form>

      <el-divider content-position="left">返回结果</el-divider>
      <pre class="result">{{ result }}</pre>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'DingTalkSheets',
  data() {
    return {
      workbookId: '',
      range: 'Sheet1!A1:D10',
      valuesText: '[]',
      result: '暂无数据',
      loading: false
    }
  },
  created() {
    this.workbookId = this.$route.query.workbookId || ''
  },
  methods: {
    async listSheets() {
      if (!this.checkWorkbook()) return
      await this.request(this.$http.get('/api/third-party/dingtalk/sheets', {
        params: { workbookId: this.workbookId }
      }))
    },
    async getRange() {
      if (!this.checkWorkbook() || !this.checkRange()) return
      await this.request(this.$http.get('/api/third-party/dingtalk/sheets/range', {
        params: { workbookId: this.workbookId, range: this.range }
      }))
    },
    async updateRange() {
      if (!this.checkWorkbook() || !this.checkRange()) return
      let values
      try {
        values = JSON.parse(this.valuesText)
      } catch (e) {
        this.$message.error('写入数据不是合法的 JSON 数组')
        return
      }
      await this.request(this.$http.put('/api/third-party/dingtalk/sheets/range', {
        workbookId: this.workbookId,
        range: this.range,
        values
      }))
    },
    async request(promise) {
      this.loading = true
      try {
        const res = await promise
        if (res.data.code === 200) {
          this.result = JSON.stringify(res.data.data, null, 2)
          this.$message.success(res.data.message || '操作成功')
        } else {
          this.$message.error(res.data.message || '操作失败')
        }
      } catch (err) {
        const message = (err.response && err.response.data && err.response.data.message) || err.message
        this.result = message
        if (!err.response || err.response.status !== 401) {
          this.$message.error(message)
        }
      } finally {
        this.loading = false
      }
    },
    checkWorkbook() {
      if (!this.workbookId) {
        this.$message.warning('请先填写 workbookId')
        return false
      }
      return true
    },
    checkRange() {
      if (!this.range) {
        this.$message.warning('请先填写 range')
        return false
      }
      return true
    }
  }
}
</script>

<style scoped>
.sheet-page {
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
.write-btn {
  margin-top: 10px;
}
.result {
  max-height: 320px;
  overflow: auto;
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
