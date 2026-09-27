<template>
  <el-card
    :shadow="variant === 'trash' ? 'never' : 'hover'"
    class="todo-card"
    :class="cardClass"
  >
    <template #header>
      <div class="todo-header">
        <div class="todo-title">
          <el-checkbox
            v-if="variant !== 'trash'"
            :model-value="todo.completed"
            :title="variant === 'done' ? '取消完成' : '标记完成 / 取消完成'"
            @change="$emit('toggle', todo)"
          />
          <el-tag v-if="todo.top" type="warning" size="small">置顶</el-tag>
          <el-tag v-if="variant === 'pending' && todo.daily" type="success" size="small">每日</el-tag>
          <el-tag v-if="variant === 'done'" type="success" size="small" effect="dark">已完成</el-tag>
          <el-tag v-else-if="variant === 'pending'" :type="statusType" size="small" effect="dark">
            {{ todo.status }}
          </el-tag>
          <span class="todo-title-text" :class="{ 'done-title': variant === 'done' }">{{ todo.title }}</span>
        </div>

        <div class="todo-actions">
          <el-tag
            v-if="todo.label"
            :type="variant === 'trash' ? 'info' : 'primary'"
            effect="plain"
            size="small"
          >{{ todo.label }}</el-tag>

          <!-- 进行中 / 已完成 -->
          <template v-if="variant !== 'trash'">
            <el-button v-if="variant === 'pending'" link type="primary" @click="$emit('detail', todo)">详情</el-button>
            <el-button
              link
              :type="variant === 'done' ? 'warning' : 'success'"
              @click="$emit('toggle', todo)"
            >{{ variant === 'done' ? '取消完成' : '标记完成' }}</el-button>
            <el-button link type="primary" @click="$emit('edit', todo)">编辑</el-button>
            <el-button link type="danger" @click="$emit('remove', todo)">删除</el-button>
          </template>

          <!-- 回收站 -->
          <template v-else>
            <el-button link type="success" @click="$emit('restore', todo)">恢复</el-button>
            <el-button link type="danger" @click="$emit('permanent-remove', todo)">彻底删除</el-button>
          </template>
        </div>
      </div>
    </template>

    <div class="todo-time">
      <span>开始：{{ formatMinute(todo.startTime) }}</span>
      <span>结束：{{ formatMinute(todo.endTime) }}</span>
      <template v-if="variant === 'pending'">
        <span v-if="todo.remindTime" class="remind-tip">
          <el-icon><AlarmClock /></el-icon>提醒：{{ formatMinute(todo.remindTime) }}
          <el-tag v-if="todo.reminded" size="small" type="success" effect="plain">已提醒</el-tag>
        </span>
        <span v-if="todo.daily && todo.repeatUntil" class="repeat-tip">每日重复至 {{ todo.repeatUntil }}</span>
        <span v-else-if="todo.daily" class="repeat-tip">每天重复，长期有效</span>
      </template>
    </div>

    <div v-if="variant === 'pending' && todo.content" class="todo-content" v-html="todo.content"></div>

    <div class="todo-meta">
      创建于 {{ formatMinute(todo.createdAt) }} · 修改于 {{ formatMinute(todo.updatedAt) }}
    </div>
  </el-card>
</template>

<script>
import { AlarmClock } from '@element-plus/icons-vue'
import { formatMinute } from '../utils/date'

/**
 * 待办卡片（进行中 / 已完成 / 回收站 三种形态）。
 * variant: 'pending' 进行中 | 'done' 已完成 | 'trash' 回收站
 */
export default {
  name: 'TodoCard',
  components: { AlarmClock },
  props: {
    todo: {
      type: Object,
      required: true
    },
    variant: {
      type: String,
      default: 'pending',
      validator: value => ['pending', 'done', 'trash'].includes(value)
    }
  },
  emits: ['detail', 'toggle', 'edit', 'remove', 'restore', 'permanent-remove'],
  computed: {
    cardClass() {
      if (this.variant === 'trash') return 'trash-todo-card'
      return {
        'todo-done': this.variant === 'done',
        'todo-overdue': this.variant === 'pending' && this.todo.status === '逾期'
      }
    },
    statusType() {
      if (this.todo.status === '已完成') return 'success'
      if (this.todo.status === '逾期') return 'danger'
      return 'primary'
    }
  },
  methods: {
    formatMinute
  }
}
</script>

<style scoped>
.todo-card {
  margin-bottom: 12px;
  text-align: left;
}
/* 逾期任务：卡片左侧红色提醒条 */
.todo-overdue {
  border-left: 4px solid var(--el-color-danger);
}
/* 已完成任务：整体淡化 */
.todo-done {
  opacity: 0.75;
}
.todo-done {
  cursor: default;
}
.trash-todo-card {
  opacity: 0.85;
  border-style: dashed;
}
.todo-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.todo-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  min-width: 0;
}
.todo-title-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.done-title {
  text-decoration: line-through;
  color: var(--el-text-color-placeholder);
}
.todo-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
.todo-time {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}
.repeat-tip {
  color: var(--el-color-success);
}
.remind-tip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--el-color-warning);
}
.todo-content {
  padding: 10px 12px;
  border-left: 3px solid var(--el-border-color);
  background: var(--el-fill-color-light);
  border-radius: 0 4px 4px 0;
  line-height: 1.7;
  margin-bottom: 8px;
}
.todo-content :deep(img) {
  max-width: 100%;
}
.todo-content :deep(blockquote) {
  margin: 6px 0;
  padding-left: 10px;
  border-left: 3px solid var(--el-border-color);
  color: var(--el-text-color-secondary);
}
.todo-meta {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
</style>
