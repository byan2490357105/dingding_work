<template>
  <el-card class="setting-card">
    <template #header>
      <div class="card-header">
        <div class="header-left">
          <el-icon><Bell /></el-icon>
          <span>消息提示</span>
          <el-badge v-if="unreadCount > 0" :value="unreadCount" :max="99" class="badge" />
        </div>
        <div class="header-right">
          <el-button size="small" @click="loadList">刷新</el-button>
          <el-button size="small" type="primary" plain :disabled="unreadCount === 0" @click="markAllRead">全部已读</el-button>
        </div>
      </div>
    </template>

    <div v-loading="loading" class="notification-list">
      <div
        v-for="item in list"
        :key="item.id"
        class="notification-item"
        :class="{ unread: !item.isRead }"
        @click="toggleExpand(item)"
      >
        <div class="item-header">
          <span class="type-icon">{{ typeIcon(item.type) }}</span>
          <span class="item-title">{{ item.title }}</span>
          <span class="item-time">{{ formatTime(item.createdAt) }}</span>
          <span v-if="!item.isRead" class="unread-dot"></span>
        </div>
        <div v-if="expandedId === item.id" class="item-content" v-html="item.content"></div>
      </div>
      <el-empty v-if="!loading && list.length === 0" description="暂无消息" :image-size="80"></el-empty>
    </div>

    <div v-if="total > pageSize" class="pagination-wrap">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="pageNum"
        @current-change="onPageChange"
      ></el-pagination>
    </div>
  </el-card>
</template>

<script>
import { Bell } from '@element-plus/icons-vue'

export default {
  name: 'NotificationCard',
  components: { Bell },
  data() {
    return {
      list: [],
      loading: false,
      pageNum: 1,
      pageSize: 10,
      total: 0,
      unreadCount: 0,
      expandedId: null
    }
  },
  created() {
    this.loadList()
    this.loadUnreadCount()
  },
  methods: {
    async loadList() {
      this.loading = true
      try {
        const res = await this.$http.get('/api/notifications', {
          params: { pageNum: this.pageNum, pageSize: this.pageSize }
        })
        if (res.data.code === 200) {
          const data = res.data.data
          this.list = data.records || []
          this.total = data.total || 0
        }
      } catch (err) {
        // 静默
      } finally {
        this.loading = false
      }
    },
    async loadUnreadCount() {
      try {
        const res = await this.$http.get('/api/notifications/unread-count')
        if (res.data.code === 200) {
          this.unreadCount = res.data.data.count || 0
        }
      } catch (err) {
        // 静默
      }
    },
    async toggleExpand(item) {
      if (this.expandedId === item.id) {
        this.expandedId = null
        return
      }
      this.expandedId = item.id
      // 自动标记已读
      if (!item.isRead) {
        try {
          await this.$http.put(`/api/notifications/${item.id}/read`)
          item.isRead = true
          this.unreadCount = Math.max(0, this.unreadCount - 1)
        } catch (err) {
          // 静默
        }
      }
    },
    async markAllRead() {
      try {
        await this.$http.put('/api/notifications/read-all')
        this.list.forEach(item => { item.isRead = true })
        this.unreadCount = 0
        this.$message.success('已全部标记为已读')
      } catch (err) {
        this.$message.error('操作失败')
      }
    },
    onPageChange(page) {
      this.pageNum = page
      this.expandedId = null
      this.loadList()
    },
    typeIcon(type) {
      const map = {
        TODO_REMIND: '⏰',
        TODO_OVERDUE: '⚠',
        AI_REPORT: '📊'
      }
      return map[type] || '📢'
    },
    formatTime(time) {
      if (!time) return ''
      return time.replace('T', ' ').substring(0, 16)
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
.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.header-right {
  display: flex;
  gap: 8px;
}
.badge {
  margin-left: 4px;
}
.notification-list {
  min-height: 200px;
}
.notification-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s;
}
.notification-item:hover {
  background-color: #f9f9f9;
}
.notification-item.unread {
  background-color: #ecf5ff;
}
.item-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.type-icon {
  font-size: 16px;
}
.item-title {
  flex: 1;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-time {
  font-size: 12px;
  color: #999;
  flex-shrink: 0;
}
.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #f56c6c;
  flex-shrink: 0;
}
.item-content {
  margin-top: 10px;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 4px;
  font-size: 14px;
  line-height: 1.6;
}
.item-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  font-size: 13px;
}
.item-content :deep(th),
.item-content :deep(td) {
  border: 1px solid #ddd;
  padding: 6px;
  text-align: left;
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
</style>
