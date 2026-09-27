<template>
  <!-- AI 报告弹窗 + 历史报告抽屉：Notes / Todo 共用一套 -->
  <el-dialog
    v-model="visible"
    :title="title"
    width="720px"
    :close-on-click-modal="false"
  >
    <div v-loading="loading">
      <div v-if="periodText" class="report-period">统计周期：{{ periodText }}</div>
      <div v-if="cached" class="report-cached-tip">（来自缓存，数据未更新）</div>
      <div class="report-content" v-html="html"></div>
    </div>
    <template #footer>
      <el-button type="primary" plain @click="openHistory">历史报告</el-button>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>

  <el-drawer v-model="historyVisible" title="AI 报告历史" size="460px" :append-to-body="true">
    <div class="history-toolbar">
      <el-radio-group v-model="historyType" @change="loadHistory">
        <el-radio-button label="monthly">月报</el-radio-button>
        <el-radio-button label="weekly">周报</el-radio-button>
      </el-radio-group>
      <el-button size="small" @click="loadHistory">刷新</el-button>
    </div>
    <div v-loading="historyLoading" class="history-body">
      <el-empty v-if="historyList.length === 0" description="暂无历史报告" :image-size="60" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="rep in historyList"
          :key="rep.id"
          :timestamp="formatMinute(rep.generatedAt)"
          placement="top"
        >
          <el-card shadow="hover" class="history-card" @click="showHistoryReport(rep)">
            <div class="history-period">{{ rep.period }}</div>
            <div class="history-type">类型：{{ reportTypeLabel(rep.reportType) }}</div>
            <div class="history-click-tip">点击查看完整报告</div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </div>
  </el-drawer>
</template>

<script>
import { aiApi } from '../api/ai'
import { renderMarkdown } from '../utils/markdown'
import { formatMinute } from '../utils/date'
import { notifyError } from '../utils/notify'

const REPORT_LABEL = {
  'note-weekly': '笔记周报',
  'note-monthly': '笔记月报',
  'todo-weekly': '待办周报',
  'todo-monthly': '待办月报'
}

/**
 * AI 报告面板：一个组件同时承载「报告弹窗」与「历史报告抽屉」。
 *
 * 父组件用法：
 *   <ai-report-panel ref="reportPanel" kind="note" />
 *   this.$refs.reportPanel.open('weekly')   // monthly | weekly
 */
export default {
  name: 'AiReportPanel',
  props: {
    /** note=笔记分析 / todo=待办分析 */
    kind: {
      type: String,
      default: 'note'
    }
  },
  data() {
    return {
      visible: false,
      loading: false,
      periodText: '',
      cached: false,
      html: '',
      // monthly / weekly
      period: 'monthly',
      historyVisible: false,
      historyLoading: false,
      historyList: [],
      historyType: 'monthly'
    }
  },
  computed: {
    kindLabel() {
      return this.kind === 'todo' ? '待办' : '笔记'
    },
    title() {
      return `AI ${this.kindLabel}${this.period === 'weekly' ? '周报' : '月报'}`
    },
    /** 当前 kind+周期 对应的后端报告类型编码 */
    reportType() {
      return `${this.kind === 'todo' ? 'todo' : 'note'}-${this.period === 'weekly' ? 'weekly' : 'monthly'}`
    }
  },
  methods: {
    formatMinute,

    /**
     * 打开报告弹窗
     * @param {'monthly'|'weekly'} period
     */
    open(period = 'monthly') {
      this.period = period
      this.visible = true
      this.generate()
    },

    async generate() {
      this.loading = true
      this.cached = false
      this.periodText = ''
      this.html = `<div style="color:#909399">AI 正在分析你近一${this.period === 'weekly' ? '周' : '个月'}的${this.kindLabel}，请稍候...</div>`
      try {
        const loader =
          this.kind === 'todo'
            ? this.period === 'weekly' ? aiApi.todoWeekly : aiApi.todoMonthly
            : this.period === 'weekly' ? aiApi.noteWeekly : aiApi.noteMonthly
        const data = await loader()
        this.periodText = data.period || ''
        this.cached = !!data.cached
        this.html = renderMarkdown(data.content || '')
      } catch (err) {
        const msg = (err.response && err.response.data && err.response.data.message) || err.message || '生成失败，请稍后重试'
        this.html = `<div style="color:#f56c6c">${msg}</div>`
      } finally {
        this.loading = false
      }
    },

    openHistory() {
      // 抽屉里的月/周报切换默认对齐当前报告周期
      this.historyType = this.period
      this.historyVisible = true
      this.loadHistory()
    },

    async loadHistory() {
      this.historyLoading = true
      try {
        const type = `${this.kind === 'todo' ? 'todo' : 'note'}-${this.historyType}`
        this.historyList = (await aiApi.history(type, 12)) || []
      } catch (err) {
        notifyError(err, '加载历史报告失败')
      } finally {
        this.historyLoading = false
      }
    },

    showHistoryReport(rep) {
      this.period = rep.reportType && rep.reportType.endsWith('weekly') ? 'weekly' : 'monthly'
      this.periodText = ''
      this.cached = false
      this.html = renderMarkdown(rep.content || '')
      this.historyVisible = false
      this.visible = true
    },

    reportTypeLabel(type) {
      return REPORT_LABEL[type] || type || '未知'
    }
  }
}
</script>

<style scoped>
.report-period {
  font-size: 13px;
  color: #909399;
  margin-bottom: 12px;
}
.report-cached-tip {
  font-size: 12px;
  color: #e6a23c;
  background: #fdf6ec;
  border: 1px solid #f5dab1;
  border-radius: 4px;
  padding: 4px 10px;
  margin-bottom: 12px;
}
.report-content {
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 8px;
  line-height: 1.8;
  font-size: 14px;
  color: #303133;
}
.report-content :deep(h2) {
  font-size: 17px;
  color: #2e74b5;
  margin: 18px 0 10px;
  padding-bottom: 6px;
  border-bottom: 2px solid #e4e7ed;
}
.report-content :deep(h3) {
  font-size: 15px;
  color: #409eff;
  margin: 14px 0 8px;
}
.report-content :deep(strong) {
  color: #303133;
}
.report-content :deep(p) {
  margin: 8px 0;
}
.report-content :deep(ul),
.report-content :deep(ol) {
  margin: 8px 0;
  padding-left: 24px;
}
.report-content :deep(li) {
  margin: 4px 0;
}
.history-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.history-body {
  padding: 0 4px;
}
.history-card {
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
}
.history-card:hover {
  transform: translateY(-2px);
  border-color: var(--el-color-primary);
}
.history-period {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}
.history-type {
  font-size: 12px;
  color: #909399;
}
.history-click-tip {
  font-size: 12px;
  color: var(--el-color-primary);
  margin-top: 6px;
}
</style>
