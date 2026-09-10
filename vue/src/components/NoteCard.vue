<template>
  <el-card class="note-card" shadow="hover">
    <div class="note-head">
      <span class="note-title note-title-link" :title="note.title" @click="$emit('view', note)">{{ note.title }}</span>
      <el-tag v-if="note.tag" size="small" type="success" effect="plain">{{ note.tag }}</el-tag>
    </div>

    <!-- 富文本内容（内容为本人编辑，v-html 直接渲染） -->
    <div class="note-content" v-html="note.content" @click="$emit('view', note)"></div>

    <div class="note-foot">
      <span class="note-time">{{ formatTime(note.updateTime) }}</span>
      <div class="note-actions">
        <el-button size="small" @click="$emit('view', note)">详情</el-button>
        <el-button
          size="small"
          :type="note.isPinned === 1 ? 'warning' : 'default'"
          @click="$emit('toggle-pin', note)"
        >
          {{ note.isPinned === 1 ? '取消置顶' : '置顶' }}
        </el-button>
        <el-button size="small" type="primary" @click="$emit('edit', note)">编辑</el-button>
        <el-button size="small" type="danger" @click="$emit('remove', note)">删除</el-button>
      </div>
    </div>
  </el-card>
</template>

<script>
export default {
  name: 'NoteCard',
  props: {
    note: {
      type: Object,
      required: true
    }
  },
  emits: ['edit', 'toggle-pin', 'remove'],
  methods: {
    // 时间精确到分钟展示，兼容 ISO 字符串与数组两种序列化格式
    formatTime(t) {
      if (!t) return ''
      // 后端 LocalDateTime 可能序列化为数组 [年,月,日,时,分]
      if (Array.isArray(t)) {
        const [y, m, d, hh = 0, mm = 0] = t
        return `${y}-${pad(m)}-${pad(d)} ${pad(hh)}:${pad(mm)}`
      }
      // ISO 字符串：2026-09-09T14:30:00 -> 2026-09-09 14:30
      return String(t).replace('T', ' ').slice(0, 16)
    }
  }
}

function pad(n) {
  return String(n).padStart(2, '0')
}
</script>

<style scoped>
.note-card {
  margin-bottom: 16px;
}
.note-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}
.note-title {
  font-weight: 600;
  font-size: 15px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.note-title-link {
  cursor: pointer;
}
.note-title-link:hover {
  color: var(--el-color-primary);
}
.note-content {
  cursor: pointer;
  min-height: 40px;
  max-height: 160px;
  overflow: hidden;
  color: #606266;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}
.note-content :deep(ul),
.note-content :deep(ol) {
  padding-left: 20px;
  margin: 4px 0;
}
.note-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 8px;
  border-top: 1px solid #ebeef5;
}
.note-time {
  color: #909399;
  font-size: 12px;
}
.note-actions {
  display: flex;
  gap: 6px;
}
</style>
