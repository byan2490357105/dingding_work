<template>
  <div v-loading="loading" class="timeline-view">
    <!-- 时间范围筛选：全部 / 按年 / 按月 / 自定义时间段 -->
    <div class="timeline-toolbar">
      <el-radio-group v-model="mode" size="default">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button label="year">按年</el-radio-button>
        <el-radio-button label="month">按月</el-radio-button>
        <el-radio-button label="custom">时间段</el-radio-button>
      </el-radio-group>
      <el-date-picker
        v-if="mode === 'year'"
        v-model="yearValue"
        type="year"
        placeholder="选择年份"
        format="YYYY 年"
        value-format="YYYY"
        :clearable="false"
        style="width: 140px"
      />
      <el-date-picker
        v-if="mode === 'month'"
        v-model="monthValue"
        type="month"
        placeholder="选择年月"
        format="YYYY 年 MM 月"
        value-format="YYYY-MM"
        :clearable="false"
        style="width: 160px"
      />
      <template v-if="mode === 'custom'">
        <el-date-picker
          v-model="customRange"
          type="daterange"
          range-separator="至"
          start-placeholder="起始日期"
          end-placeholder="截止日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          :clearable="false"
          unlink-panels
          style="width: 300px"
        />
        <el-button link type="primary" @click="resetCustomRange">恢复默认范围</el-button>
      </template>
      <span class="timeline-range-tip">
        当前范围：{{ rangeText }} · {{ timelineTodos.length }} 个未完成事务与所选范围有交集
      </span>
    </div>

    <el-empty v-if="timelineTodos.length === 0" :description="emptyText" :image-size="90"></el-empty>
    <template v-else>
      <!-- 甘特总览：不同事务使用不同颜色 -->
      <div class="gantt-card">
        <div class="view-title">
          时间线总览
          <span class="view-sub">
            {{ rangeText }} · 共 {{ timelineTodos.length }} 个未完成事务 · 不同事务使用不同颜色
          </span>
        </div>
        <div class="gantt-wrap">
          <div class="gantt-labels">
            <div class="gantt-axis-spacer"></div>
            <div
              v-for="item in timelineTodos"
              :key="'label-' + item.id"
              class="gantt-label"
              :title="item.title"
            >
              <span class="gantt-dot" :style="{ background: item.color }"></span>
              <span class="gantt-label-text">{{ item.title }}</span>
            </div>
          </div>
          <div class="gantt-area">
            <div class="gantt-axis">
              <span
                v-for="(tick, ti) in ganttTicks"
                :key="'tick-' + ti"
                class="gantt-tick"
                :style="{ left: tick.left + '%' }"
              >{{ tick.label }}</span>
              <span
                v-if="todayLeft !== null"
                class="gantt-today-badge"
                :style="{ left: todayLeft + '%' }"
              >今天</span>
            </div>
            <div v-for="item in timelineTodos" :key="'bar-' + item.id" class="gantt-track">
              <div
                class="gantt-bar"
                :style="{ left: item.left + '%', width: item.width + '%', background: item.color }"
                :title="item.title + '：' + formatMinute(item.startTime) + ' ~ ' + formatMinute(item.endTime)"
              >
                <span class="gantt-bar-text">
                  {{ formatMinute(item.startTime).slice(5, 16) }} ~ {{ formatMinute(item.endTime).slice(5, 16) }}
                </span>
              </div>
            </div>
            <div
              v-if="todayLeft !== null"
              class="gantt-today-line"
              :style="{ left: todayLeft + '%' }"
            ></div>
          </div>
        </div>
      </div>

      <!-- 时间节点：开始 / 截止按时间先后排列 -->
      <div class="milestone-card">
        <div class="view-title">
          时间节点
          <span class="view-sub">按事件发生时间先后排列 · 点击可查看详情</span>
        </div>
        <el-timeline class="milestone-timeline">
          <el-timeline-item
            v-for="(ev, index) in milestoneEvents"
            :key="index"
            :color="ev.todo.color"
            :timestamp="ev.dateText"
            :hollow="ev.type === 'end'"
          >
            <div class="milestone-item" @click="$emit('detail', ev.todo)">
              <span class="milestone-date">{{ ev.shortDate }}</span>
              <el-tag size="small" :type="ev.type === 'start' ? 'success' : 'danger'" effect="dark">
                {{ ev.type === 'start' ? '开始' : '截止' }}
              </el-tag>
              <span class="milestone-title" :style="{ color: ev.todo.color }">{{ ev.todo.title }}</span>
              <el-tag v-if="ev.todo.label" size="small" effect="plain" type="info">{{ ev.todo.label }}</el-tag>
              <span class="milestone-time">{{ ev.timeText }}</span>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </template>
  </div>
</template>

<script>
import { todoApi } from '../api/todo'
import { formatMinute, parseLocal, toDateKey } from '../utils/date'
import { notifyError } from '../utils/notify'

/** 不同事务的绘制颜色（循环取用） */
const TIMELINE_COLORS = [
  '#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#9254DE',
  '#13C2C2', '#EB2F96', '#FAAD14', '#2F54EB', '#7CB305'
]

const pad = n => String(n).padStart(2, '0')

/**
 * 待办时间轴视图（甘特总览 + 时间节点）。
 *
 * 只请求未完成待办，且按选中范围（全部 / 年 / 月 / 自定义）向后端取交集数据，
 * 不再依赖父组件的全量列表。
 */
export default {
  name: 'TodoTimeline',
  emits: ['detail'],
  data() {
    const now = new Date()
    return {
      mode: 'all',
      yearValue: String(now.getFullYear()),
      monthValue: `${now.getFullYear()}-${pad(now.getMonth() + 1)}`,
      customRange: null,
      loading: false,
      // 当前区间内的未完成待办
      todos: [],
      // 已知的最晚截止日期（用于自定义范围默认值）
      knownMaxEnd: ''
    }
  },
  computed: {
    /** 当前模式对应的时间窗口（左闭右开，统一为 Date）；null = 全部 */
    timeWindow() {
      if (this.mode === 'year') {
        const y = Number(this.yearValue) || new Date().getFullYear()
        return { start: new Date(y, 0, 1), end: new Date(y + 1, 0, 1) }
      }
      if (this.mode === 'month') {
        if (!this.monthValue) return null
        const [y, m] = this.monthValue.split('-').map(Number)
        if (!y || !m) return null
        return { start: new Date(y, m - 1, 1), end: new Date(y, m, 1) }
      }
      if (this.mode === 'custom') {
        const r = this.customRange
        if (!r || !r[0] || !r[1]) return null
        const [sy, sm, sd] = r[0].split('-').map(Number)
        const [ey, em, ed] = r[1].split('-').map(Number)
        // 结束日期含当天：上界取次日 0 点
        return { start: new Date(sy, sm - 1, sd), end: new Date(ey, em - 1, ed + 1) }
      }
      return null
    },
    rangeText() {
      if (this.mode === 'year') return this.yearValue + ' 年'
      if (this.mode === 'month') {
        const parts = String(this.monthValue).split('-')
        return parts[0] + ' 年 ' + Number(parts[1]) + ' 月'
      }
      if (this.mode === 'custom' && this.customRange) {
        return this.customRange[0] + ' ~ ' + this.customRange[1]
      }
      return '全部时间'
    },
    // 未完成待办（含毫秒时间戳，按开始时间升序）
    items() {
      const items = []
      this.todos.forEach(todo => {
        if (todo.completed || !todo.startTime || !todo.endTime) return
        const s = parseLocal(todo.startTime).getTime()
        const e = parseLocal(todo.endTime).getTime()
        if (isNaN(s) || isNaN(e)) return
        items.push({ todo, s, e })
      })
      items.sort((a, b) => a.s - b.s)
      return items
    },
    // 甘特横轴范围：有窗口用窗口；「全部」时取最早开始 ~ 最晚截止（当天对齐）
    ganttRange() {
      const win = this.timeWindow
      if (win) {
        const start = new Date(win.start.getFullYear(), win.start.getMonth(), win.start.getDate()).getTime()
        const end = new Date(win.end.getFullYear(), win.end.getMonth(), win.end.getDate()).getTime()
        return { start, end, span: Math.max(1, end - start) }
      }
      let min = Infinity
      let max = -Infinity
      this.items.forEach(it => {
        if (it.s < min) min = it.s
        if (it.e > max) max = it.e
      })
      if (min === Infinity || max === -Infinity) return null
      const startDate = new Date(min)
      const endDate = new Date(max)
      const start = new Date(startDate.getFullYear(), startDate.getMonth(), startDate.getDate()).getTime()
      const end = new Date(endDate.getFullYear(), endDate.getMonth(), endDate.getDate() + 1).getTime()
      return { start, end, span: Math.max(1, end - start) }
    },
    // 分配颜色并计算甘特条位置
    timelineTodos() {
      const range = this.ganttRange
      return this.items.map((it, index) => {
        let left = 0
        let width = 100
        if (range) {
          left = Math.min(100, Math.max(0, (it.s - range.start) / range.span * 100))
          const right = Math.min(100, Math.max(0, (it.e - range.start) / range.span * 100))
          width = Math.max(1.5, right - left)
        }
        return Object.assign({}, it.todo, {
          color: TIMELINE_COLORS[index % TIMELINE_COLORS.length],
          left,
          width
        })
      })
    },
    // 甘特横轴刻度（按跨度自适应 4~6 等分）
    ganttTicks() {
      const range = this.ganttRange
      if (!range) return []
      const days = range.span / (24 * 3600 * 1000)
      const divisions = days > 320 ? 6 : days > 120 ? 5 : 4
      const withYear = days > 200
      const ticks = []
      for (let i = 0; i <= divisions; i++) {
        const d = new Date(range.start + range.span * i / divisions)
        const label = withYear
          ? d.getFullYear() + '年' + (d.getMonth() + 1) + '月' + d.getDate() + '日'
          : (d.getMonth() + 1) + '月' + d.getDate() + '日'
        ticks.push({ left: i * (100 / divisions), label })
      }
      return ticks
    },
    // 「今天」竖线位置
    todayLeft() {
      const range = this.ganttRange
      if (!range) return null
      const now = Date.now()
      if (now < range.start || now > range.end) return null
      return (now - range.start) / range.span * 100
    },
    // 时间节点事件流：开始 / 截止混排
    milestoneEvents() {
      const events = []
      this.timelineTodos.forEach(todo => {
        events.push({ todo, type: 'start', time: String(todo.startTime) })
        events.push({ todo, type: 'end', time: String(todo.endTime) })
      })
      events.sort((a, b) => {
        const diff = parseLocal(a.time).getTime() - parseLocal(b.time).getTime()
        if (diff !== 0) return diff
        if (a.type !== b.type) return a.type === 'start' ? -1 : 1
        return String(a.todo.title).localeCompare(String(b.todo.title))
      })
      return events.map(ev => {
        const d = parseLocal(ev.time)
        return Object.assign({}, ev, {
          dateText: `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`,
          shortDate: (d.getMonth() + 1) + '月' + d.getDate() + '号',
          timeText: pad(d.getHours()) + ':' + pad(d.getMinutes())
        })
      })
    },
    emptyText() {
      if (this.mode === 'all') return '暂无未完成的待办任务'
      return '所选时间范围内没有未完成的待办，可调整范围或切换"全部"查看'
    }
  },
  watch: {
    // 范围变化（含模式切换）后按新窗口重新拉取
    timeWindow: {
      handler() {
        this.load()
      },
      deep: true
    },
    // 切到自定义时间段时，按默认规则初始化范围
    mode(mode) {
      if (mode === 'custom' && !this.customRange) {
        this.resetCustomRange()
      }
    }
  },
  created() {
    this.load()
  },
  methods: {
    formatMinute,
    async load() {
      this.loading = true
      try {
        const win = this.timeWindow
        const params = { completed: false, overlap: true, limit: 500 }
        if (win) {
          params.start = toDateKey(win.start)
          // 结束日期含当天，因此窗口终点回退一天
          params.end = toDateKey(new Date(win.end.getTime() - 86400000))
        } else {
          params.overlap = false
        }
        this.todos = (await todoApi.range(params)) || []
        // 记录最晚截止日期，供自定义范围默认值使用
        let maxEnd = this.knownMaxEnd
        this.todos.forEach(t => {
          const day = t.endTime ? String(t.endTime).slice(0, 10) : ''
          if (day && day > maxEnd) maxEnd = day
        })
        this.knownMaxEnd = maxEnd
      } catch (err) {
        notifyError(err, '加载时间轴数据失败')
      } finally {
        this.loading = false
      }
    },
    /** 供父组件在增删改后调用 */
    refresh() {
      return this.load()
    },
    /** 自定义范围默认：今天 ~ 已加载数据中最晚的截止日期（缺失则今天+30 天） */
    async resetCustomRange() {
      const today = toDateKey(new Date())
      if (!this.knownMaxEnd) {
        await this.load()
      }
      const fallback = toDateKey(new Date(Date.now() + 30 * 86400000))
      this.customRange = [
        today,
        this.knownMaxEnd && this.knownMaxEnd > today ? this.knownMaxEnd : fallback
      ]
    }
  }
}
</script>

<style scoped>
.timeline-view {
  margin-bottom: 26px;
}
.timeline-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  background: #fff;
  border: 1px solid var(--wb-border, #ebeef5);
  border-radius: 12px;
  padding: 12px 16px;
  box-shadow: var(--wb-card-shadow, var(--el-box-shadow-light));
  margin-bottom: 18px;
}
.timeline-range-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-left: auto;
}
.gantt-card,
.milestone-card {
  background: #fff;
  border: 1px solid var(--wb-border, #ebeef5);
  border-radius: 12px;
  padding: 18px 20px;
  box-shadow: var(--wb-card-shadow, var(--el-box-shadow-light));
  margin-bottom: 18px;
}
.view-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 14px;
}
.view-sub {
  font-size: 12px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
}
.gantt-wrap {
  display: flex;
}
.gantt-labels {
  width: 180px;
  flex-shrink: 0;
}
.gantt-axis-spacer {
  height: 28px;
}
.gantt-label {
  height: 36px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding-right: 10px;
  font-size: 13px;
  color: var(--el-text-color-primary);
}
.gantt-label-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.gantt-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.gantt-area {
  flex: 1;
  position: relative;
  min-width: 0;
}
.gantt-axis {
  height: 28px;
  position: relative;
  border-bottom: 1px dashed var(--el-border-color);
}
.gantt-tick {
  position: absolute;
  bottom: 4px;
  transform: translateX(-50%);
  font-size: 12px;
  color: var(--el-text-color-secondary);
  background: #fff;
  padding: 0 4px;
}
.gantt-today-badge {
  position: absolute;
  top: 2px;
  transform: translateX(-50%);
  font-size: 11px;
  line-height: 16px;
  color: #fff;
  background: var(--el-color-danger);
  border-radius: 8px;
  padding: 0 6px;
}
.gantt-track {
  height: 36px;
  position: relative;
}
.gantt-track:hover {
  background: var(--el-fill-color-lighter);
}
.gantt-bar {
  position: absolute;
  top: 8px;
  height: 20px;
  min-width: 24px;
  border-radius: 10px;
  overflow: hidden;
  line-height: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.18);
}
.gantt-bar-text {
  display: block;
  padding: 0 8px;
  font-size: 11px;
  color: #fff;
  white-space: nowrap;
}
.gantt-today-line {
  position: absolute;
  top: 28px;
  bottom: 0;
  width: 0;
  border-left: 2px dashed var(--el-color-danger);
  opacity: 0.7;
  pointer-events: none;
}
.milestone-timeline {
  padding: 4px 4px 0;
}
.milestone-timeline :deep(.el-timeline-item__node) {
  width: 14px;
  height: 14px;
}
.milestone-item {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 7px 12px;
  border-radius: 6px;
  background: var(--el-fill-color-lighter);
  transition: background 0.2s;
}
.milestone-item:hover {
  background: var(--el-fill-color);
}
.milestone-date {
  font-weight: 700;
  color: var(--el-text-color-primary);
  flex-shrink: 0;
}
.milestone-title {
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.milestone-time {
  margin-left: auto;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
}
</style>
