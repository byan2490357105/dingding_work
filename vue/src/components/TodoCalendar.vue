<template>
  <div v-loading="loading" class="calendar-view">
    <div class="calendar-card">
      <div class="calendar-toolbar">
        <el-button circle size="large" title="上一月" @click="prevMonth">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <div class="calendar-center">
          <div class="calendar-title">{{ year }} 年 {{ month + 1 }} 月</div>
          <div class="calendar-picker-row">
            <el-date-picker
              v-model="picker"
              type="month"
              placeholder="选择年月"
              format="YYYY 年 MM 月"
              value-format="YYYY-MM"
              :clearable="false"
              size="small"
              @change="onPickerChange"
            />
            <el-button link type="primary" @click="goToday">回到本月</el-button>
          </div>
        </div>
        <el-button circle size="large" title="下一月" @click="nextMonth">
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>

      <div class="calendar-grid">
        <div v-for="w in weekLabels" :key="w" class="calendar-week">{{ w }}</div>
        <div
          v-for="cell in cells"
          :key="cell.key"
          class="calendar-cell"
          :class="{ 'cell-out': !cell.inMonth, 'cell-today': cell.isToday }"
        >
          <div class="cell-day">
            <span class="cell-day-num" :class="{ today: cell.isToday }">{{ cell.day }}</span>
            <span v-if="cell.isToday" class="today-badge">今天</span>
          </div>
          <div class="cell-todos">
            <div
              v-for="todo in cell.todos"
              :key="todo.id"
              class="cell-todo"
              :class="'chip-' + chipClass(todo)"
              :title="todo.title + '（' + formatMinute(todo.startTime) + '）'"
              @click="$emit('detail', todo)"
            >
              <span class="chip-time">{{ timePart(todo.startTime) }}</span>
              <span class="chip-title">{{ todo.title }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="calendar-legend">
        <span><span class="legend-dot dot-doing"></span>进行中</span>
        <span><span class="legend-dot dot-done"></span>已完成</span>
        <span><span class="legend-dot dot-overdue"></span>逾期</span>
        <span class="legend-tip">
          任务归属日期以开始时间为准 · 同一天可挂载多条任务 · 点击任务查看详情
          <template v-if="truncated"> · 已按上限截断部分数据</template>
        </span>
      </div>
    </div>
  </div>
</template>

<script>
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { todoApi } from '../api/todo'
import { formatMinute, timePart, toDateKey, datePart } from '../utils/date'
import { notifyError } from '../utils/notify'

/**
 * 待办日历视图。
 *
 * 只请求当前展示的 6×7 网格所覆盖的日期区间（月首周一 ~ 42 天后），
 * 不再依赖父组件的全量待办列表；月份切换时按需重新拉取。
 * 父组件通过 ref 调用 refresh() 在数据变更后刷新。
 */
export default {
  name: 'TodoCalendar',
  components: { ArrowLeft, ArrowRight },
  emits: ['detail'],
  data() {
    const now = new Date()
    return {
      year: now.getFullYear(),
      month: now.getMonth(),
      picker: `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`,
      weekLabels: ['一', '二', '三', '四', '五', '六', '日'],
      loading: false,
      todos: [],
      truncated: false
    }
  },
  computed: {
    // 6×7 网格的起止日期（月首所在周的周一 ~ 第 42 天）
    gridStart() {
      const first = new Date(this.year, this.month, 1)
      let offset = first.getDay() - 1 // 周一为第一列：周日(offset=-1)归到末列
      if (offset < 0) offset = 6
      return toDateKey(new Date(this.year, this.month, 1 - offset))
    },
    gridEnd() {
      const [y, m, d] = this.gridStart.split('-').map(Number)
      return toDateKey(new Date(y, m - 1, d + 41))
    },
    // 按开始时间日期分组：一个日期可挂载多条任务
    todosByDate() {
      const map = {}
      this.todos.forEach(todo => {
        const key = datePart(todo.startTime)
        if (!key) return
        if (!map[key]) map[key] = []
        map[key].push(todo)
      })
      Object.keys(map).forEach(key => {
        map[key].sort((a, b) => String(a.startTime).localeCompare(String(b.startTime)))
      })
      return map
    },
    cells() {
      const [sy, sm, sd] = this.gridStart.split('-').map(Number)
      const todayKey = toDateKey(new Date())
      const cells = []
      for (let i = 0; i < 42; i++) {
        const d = new Date(sy, sm - 1, sd + i)
        const key = toDateKey(d)
        cells.push({
          key,
          day: d.getDate(),
          inMonth: d.getMonth() === this.month,
          isToday: key === todayKey,
          todos: this.todosByDate[key] || []
        })
      }
      return cells
    }
  },
  watch: {
    // 月份变化（含跨年切换）后按新区间重新拉取
    gridStart() {
      this.load()
    }
  },
  created() {
    this.load()
  },
  methods: {
    formatMinute,
    timePart,
    async load() {
      this.loading = true
      try {
        const list = await todoApi.range({
          start: this.gridStart,
          end: this.gridEnd,
          limit: 500
        })
        this.todos = list || []
        this.truncated = (list || []).length >= 500
      } catch (err) {
        notifyError(err, '加载日历数据失败')
      } finally {
        this.loading = false
      }
    },
    /** 供父组件在增删改后调用 */
    refresh() {
      return this.load()
    },
    prevMonth() {
      if (this.month === 0) {
        this.month = 11
        this.year -= 1
      } else {
        this.month -= 1
      }
      this.syncPicker()
    },
    nextMonth() {
      if (this.month === 11) {
        this.month = 0
        this.year += 1
      } else {
        this.month += 1
      }
      this.syncPicker()
    },
    goToday() {
      const now = new Date()
      this.year = now.getFullYear()
      this.month = now.getMonth()
      this.syncPicker()
    },
    onPickerChange(val) {
      if (!val) return
      const [y, m] = val.split('-').map(Number)
      this.year = y
      this.month = m - 1
    },
    syncPicker() {
      this.picker = `${this.year}-${String(this.month + 1).padStart(2, '0')}`
    },
    // 任务条颜色分类：已完成绿 / 逾期红 / 进行中蓝
    chipClass(todo) {
      if (todo.completed || todo.status === '已完成') return 'done'
      if (todo.status === '逾期') return 'overdue'
      return 'doing'
    }
  }
}
</script>

<style scoped>
.calendar-view {
  margin-bottom: 26px;
}
.calendar-card {
  background: var(--el-bg-color);
  border-radius: 12px;
  padding: 18px;
  box-shadow: var(--wb-card-shadow, var(--el-box-shadow-light));
  border: 1px solid var(--wb-border, #ebeef5);
}
.calendar-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.calendar-center {
  text-align: center;
}
.calendar-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}
.calendar-picker-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 6px;
}
.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
}
.calendar-week {
  text-align: center;
  padding: 8px 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
}
.calendar-cell {
  min-height: 106px;
  padding: 6px;
  border-top: 1px solid var(--el-border-color-lighter);
  border-left: 1px solid var(--el-border-color-lighter);
}
.calendar-cell:nth-child(7n + 1) {
  border-left: none;
}
.cell-out {
  background: var(--el-fill-color-lighter);
}
.cell-out .cell-day-num {
  color: var(--el-text-color-placeholder);
}
.cell-day {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}
.cell-day-num {
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.cell-day-num.today {
  background: var(--el-color-primary);
  color: #fff;
}
.today-badge {
  font-size: 11px;
  color: var(--el-color-primary);
}
.cell-todos {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 72px;
  overflow-y: auto;
}
.cell-todo {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
  border-left: 3px solid transparent;
  cursor: pointer;
  line-height: 1.5;
}
.cell-todo:hover {
  filter: brightness(0.95);
}
.chip-time {
  font-size: 11px;
  opacity: 0.85;
  flex-shrink: 0;
}
.chip-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chip-doing {
  background: var(--el-color-primary-light-9);
  border-left-color: var(--el-color-primary);
  color: var(--el-color-primary);
}
.chip-done {
  background: var(--el-color-success-light-9);
  border-left-color: var(--el-color-success);
  color: var(--el-color-success);
}
.chip-done .chip-title {
  text-decoration: line-through;
}
.chip-overdue {
  background: var(--el-color-danger-light-9);
  border-left-color: var(--el-color-danger);
  color: var(--el-color-danger);
}
.calendar-legend {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.legend-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 4px;
}
.dot-doing { background: var(--el-color-primary); }
.dot-done { background: var(--el-color-success); }
.dot-overdue { background: var(--el-color-danger); }
.legend-tip {
  margin-left: auto;
  color: var(--el-text-color-placeholder);
}
</style>
