<template>
  <div class="todo-page">
    <h2 class="page-heading">待办任务</h2>

    <div class="page-toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="searchKeyword"
          placeholder="按标题/关键字搜索待办"
          clearable
          style="width: 200px"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button @click="wordCloudVisible = true">
          <el-icon><Histogram /></el-icon>&nbsp;词云
        </el-button>
        <el-button type="primary" @click="openCreate">新建待办</el-button>
        <el-button :type="viewMode === 'calendar' ? 'primary' : 'default'" plain @click="toggleCalendarView">
          <el-icon><Calendar /></el-icon>&nbsp;{{ viewMode === 'calendar' ? '切换列表模式' : '切换日历模式' }}
        </el-button>
        <el-button :type="viewMode === 'timeline' ? 'primary' : 'default'" plain @click="toggleTimelineView">
          <el-icon><AlarmClock /></el-icon>&nbsp;{{ viewMode === 'timeline' ? '切换列表模式' : '时间轴模式' }}
        </el-button>
        <el-button :loading="loading" @click="reloadAfterChange">
          <el-icon><Refresh /></el-icon>&nbsp;刷新
        </el-button>
        <el-button type="success" plain :loading="exporting" @click="exportCsv">
          <el-icon><Download /></el-icon>&nbsp;导出待办
        </el-button>
        <el-button type="warning" @click="$refs.reportPanel.open('monthly')">
          <el-icon><MagicStick /></el-icon>&nbsp;AI 月报
        </el-button>
        <el-button type="warning" plain @click="$refs.reportPanel.open('weekly')">
          <el-icon><MagicStick /></el-icon>&nbsp;AI 周报
        </el-button>
      </div>
      <el-tag v-if="username" type="info" effect="plain">{{ username }} 的待办</el-tag>
    </div>

    <!-- 标签筛选面板（可折叠，仅列表视图显示） -->
    <tag-filter-panel
      v-model="selectedLabels"
      v-model:collapsed="tagPanelCollapsed"
      :stats="labelStats"
      :visible="viewMode === 'list'"
      hint="按标签筛选（勾选显示，取消勾选则隐藏该标签的待办）"
    />

    <el-tabs v-if="viewMode === 'list'" v-model="activeTab" class="todo-tabs" @tab-change="onTabChange">
      <el-tab-pane label="我的待办" name="todos">
        <div v-loading="loading">
          <!-- 置顶待办（当前页内，置顶排序在最前） -->
          <section class="todo-group" v-if="pendingTopRecords.length > 0">
            <div class="group-title">置顶待办（{{ pendingTopRecords.length }}）</div>
            <todo-card
              v-for="todo in pendingTopRecords"
              :key="todo.id"
              :todo="todo"
              pinned
              @detail="openDetail"
              @toggle="toggleComplete"
              @edit="openEdit"
              @remove="removeTodo"
            />
          </section>

          <!-- 未置顶的进行中待办（当前页内） -->
          <section class="todo-group">
            <div class="group-title">进行中的待办（{{ pendingNormalRecords.length }}）</div>
            <el-empty
              v-if="pendingNormalRecords.length === 0 && pendingTopRecords.length === 0"
              :description="emptyDescription"
              :image-size="70"
            />
            <todo-card
              v-for="todo in pendingNormalRecords"
              :key="todo.id"
              :todo="todo"
              @detail="openDetail"
              @toggle="toggleComplete"
              @edit="openEdit"
              @remove="removeTodo"
            />
          </section>

          <!-- 分页（进行中） -->
          <div class="pagination-bar" v-if="pendingTotal > 0">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next, jumper"
              :total="pendingTotal"
              :current-page="pendingPageNum"
              :page-size="pageSize"
              :page-sizes="[8, 12, 20, 40]"
              @current-change="onPendingPageChange"
              @size-change="onPageSizeChange"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- 已完成待办（独立分页，首次进入时才加载） -->
      <el-tab-pane :label="`已完成(${counts.done})`" name="done">
        <div v-loading="loading">
          <el-empty v-if="doneRecords.length === 0" :description="doneEmptyDescription" :image-size="70" />
          <todo-card
            v-for="todo in doneRecords"
            :key="todo.id"
            :todo="todo"
            done
            @detail="openDetail"
            @toggle="toggleComplete"
            @edit="openEdit"
            @remove="removeTodo"
          />

          <div class="pagination-bar" v-if="doneTotal > 0">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next, jumper"
              :total="doneTotal"
              :current-page="donePageNum"
              :page-size="pageSize"
              :page-sizes="[8, 12, 20, 40]"
              @current-change="onDonePageChange"
              @size-change="onPageSizeChange"
            />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="`回收站(${counts.trash})`" name="trash">
        <div v-loading="trashLoading">
          <el-empty v-if="trashTodos.length === 0" description="回收站是空的" :image-size="80" />
          <todo-card
            v-for="todo in trashTodos"
            :key="todo.id"
            :todo="todo"
            trash
            @restore="restoreTodo"
            @permanent-remove="permanentlyDeleteTodo"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 日历模式：按展示月份向后端取该区间数据 -->
    <todo-calendar v-else-if="viewMode === 'calendar'" ref="calendar" @detail="openDetail" />

    <!-- 时间轴模式：未完成待办的时间线总览 + 时间节点（按范围向后端取数） -->
    <todo-timeline v-else ref="timeline" @detail="openDetail" />

    <!-- 新建 / 编辑 弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑待办' : '新建待办'"
      width="760px"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form label-width="90px" class="todo-form">
        <el-form-item label="标题" required>
          <el-input v-model.trim="form.title" maxlength="100" show-word-limit placeholder="请输入待办标题"></el-input>
        </el-form-item>

        <el-form-item label="标签">
          <el-select
            v-model="form.label"
            filterable
            allow-create
            default-first-option
            clearable
            placeholder="选择标签或输入自定义标签"
            style="width: 100%"
          >
            <el-option v-for="label in labelOptions" :key="label" :label="label" :value="label"></el-option>
          </el-select>
          <div class="label-tip">默认标签：学习、生活、科研、出行；也可以直接输入自定义标签</div>
        </el-form-item>

        <el-form-item label="开始时间" required>
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm"
            format="YYYY-MM-DD HH:mm"
            placeholder="选择开始时间"
            style="width: 100%"
          ></el-date-picker>
        </el-form-item>

        <el-form-item label="结束时间" required>
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm"
            format="YYYY-MM-DD HH:mm"
            placeholder="选择结束/截止时间"
            style="width: 100%"
          ></el-date-picker>
        </el-form-item>

        <el-form-item label="提醒时间">
          <el-date-picker
            v-model="form.remindTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm"
            format="YYYY-MM-DD HH:mm"
            placeholder="留空不提醒，到达该时间将发送提醒"
            style="width: 100%"
          ></el-date-picker>
        </el-form-item>

        <el-form-item label="每日重复">
          <el-switch
            v-model="form.daily"
            active-text="每天重复（例如每天背单词）"
            @change="handleDailyChange"
          ></el-switch>
        </el-form-item>

        <el-form-item v-if="form.daily" label="重复截止日期">
          <el-date-picker
            v-model="form.repeatUntil"
            type="date"
            value-format="YYYY-MM-DD"
            format="YYYY-MM-DD"
            :disabled-date="repeatUntilDisabled"
            placeholder="留空表示每天重复、长期有效"
            style="width: 100%"
          ></el-date-picker>
        </el-form-item>

        <el-form-item label="置顶">
          <el-switch v-model="form.top" active-text="在列表中置顶显示"></el-switch>
        </el-form-item>

        <el-form-item label="内容">
          <RichEditor v-model="form.content" placeholder="支持加粗、斜体、列表等富文本内容..."></RichEditor>
        </el-form-item>

        <!-- AI 完成率预测 -->
        <el-form-item label="完成率预测">
          <el-button
            type="warning"
            plain
            :disabled="!canPredict"
            :loading="predicting"
            @click="predictSuccessRate"
          >
            <el-icon><MagicStick /></el-icon>&nbsp;AI 预测完成率
          </el-button>
          <div v-if="prediction" class="predict-box">
            <div class="predict-rate">
              <span class="rate-num" :class="'lv-' + prediction.level">{{ prediction.rate }}%</span>
              <el-tag :type="levelTagType" size="small" effect="dark">{{ prediction.level }}</el-tag>
            </div>
            <el-progress
              :percentage="prediction.rate"
              :color="levelColor"
              :stroke-width="10"
              :show-text="false"
            ></el-progress>
            <div class="predict-reason">{{ prediction.reason }}</div>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveTodo">保存</el-button>
      </template>
    </el-dialog>

    <!-- 待办详情弹窗 -->
    <el-dialog v-model="detailVisible" title="待办详情" width="680px">
      <div v-if="detail" class="todo-detail">
        <h2 class="detail-title">
          <el-tag :type="statusType(detail.status)" size="small" effect="dark">{{ detail.status }}</el-tag>
          <span :class="{ 'done-title': detail.completed }">{{ detail.title }}</span>
        </h2>

        <el-descriptions :column="1" border size="small" class="detail-desc">
          <el-descriptions-item label="标签">{{ detail.label || '无' }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ formatMinute(detail.startTime) }}</el-descriptions-item>
          <el-descriptions-item label="截止时间">
            <span :class="{ 'overdue-text': detail.status === '逾期' }">{{ formatMinute(detail.endTime) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="提醒时间">
            <span v-if="detail.remindTime">{{ formatMinute(detail.remindTime) }}
              <el-tag v-if="detail.reminded" size="small" type="success" effect="plain">已提醒</el-tag>
              <el-tag v-else size="small" type="warning" effect="plain">待提醒</el-tag>
            </span>
            <span v-else>无</span>
          </el-descriptions-item>
          <el-descriptions-item label="重复">
            <template v-if="detail.daily">
              每日重复<template v-if="detail.repeatUntil">，截止 {{ detail.repeatUntil }}</template>
            </template>
            <template v-else>不重复</template>
          </el-descriptions-item>
          <el-descriptions-item label="置顶">{{ detail.top ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatMinute(detail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="修改时间">{{ formatMinute(detail.updatedAt) }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">任务内容</el-divider>
        <div v-if="detail.content" class="detail-content" v-html="detail.content"></div>
        <el-empty v-else description="无任务内容" :image-size="60" />
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          :type="detail && detail.completed ? 'warning' : 'success'"
          @click="toggleCompleteFromDetail"
        >
          {{ detail && detail.completed ? '取消完成' : '标记完成' }}
        </el-button>
        <el-button type="primary" :disabled="detail && detail.completed" @click="editFromDetail">编辑</el-button>
      </template>
    </el-dialog>

    <!-- AI 待办月报 / 周报 + 历史报告 -->
    <ai-report-panel ref="reportPanel" kind="todo" />

    <!-- 词云弹窗 -->
    <word-cloud-dialog v-model="wordCloudVisible" kind="todo" />
  </div>
</template>

<script>
import { mapState } from 'vuex'
import {
  Download, MagicStick, Calendar, AlarmClock, Refresh, Search, Histogram
} from '@element-plus/icons-vue'
import RichEditor from '../components/RichEditor.vue'
import TodoCard from '../components/TodoCard.vue'
import TagFilterPanel from '../components/TagFilterPanel.vue'
import TodoCalendar from '../components/TodoCalendar.vue'
import TodoTimeline from '../components/TodoTimeline.vue'
import AiReportPanel from '../components/AiReportPanel.vue'
import WordCloudDialog from '../components/WordCloudDialog.vue'
import { todoApi } from '../api/todo'
import { aiApi } from '../api/ai'
import { formatMinute, parseLocal } from '../utils/date'
import { downloadBlob } from '../utils/download'
import { debounce } from '../composables/useSearchDebounce'
import { notifyError } from '../utils/notify'

const DEFAULT_TAGS = ['学习', '生活', '科研', '出行']

function defaultForm() {
  return {
    id: null,
    title: '',
    content: '',
    label: '',
    startTime: '',
    endTime: '',
    remindTime: '',
    daily: false,
    repeatUntil: '',
    top: false
  }
}

/** 拉取标签统计并归一化为 [{ name, count }] */
const loadLabelStats = async () =>
  ((await todoApi.labelStats()) || []).map(item => ({ name: item.label, count: item.count }))

export default {
  name: 'TodoPage',
  components: {
    Download,
    MagicStick,
    Calendar,
    AlarmClock,
    Refresh,
    Search,
    Histogram,
    RichEditor,
    TodoCard,
    TagFilterPanel,
    TodoCalendar,
    TodoTimeline,
    AiReportPanel,
    WordCloudDialog
  },
  data() {
    return {
      activeTab: 'todos',
      // 轻量计数（Tab 角标），不再为拿数字加载全量待办
      counts: { pending: 0, done: 0, total: 0, trash: 0 },
      // 列表分页（进行中 / 已完成各自独立分页）
      pendingRecords: [],
      pendingTotal: 0,
      pendingPageNum: 1,
      doneRecords: [],
      doneTotal: 0,
      donePageNum: 1,
      doneLoaded: false,
      pageSize: 8,
      // 回收站（首次进入该 Tab 时才加载）
      trashTodos: [],
      trashLoading: false,
      trashLoaded: false,
      // 全量标签统计 [{ name, count }]
      labelStats: [],
      selectedLabels: [],
      tagPanelCollapsed: false,
      // 视图模式：list 列表 / calendar 日历 / timeline 时间轴
      viewMode: 'list',
      loading: false,
      saving: false,
      exporting: false,
      dialogVisible: false,
      detailVisible: false,
      detail: null,
      form: defaultForm(),
      predicting: false,
      prediction: null,
      searchKeyword: '',
      wordCloudVisible: false
    }
  },
  computed: {
    ...mapState(['username']),
    // 预测的前置条件：标题 + 开始/结束时间都已填写
    canPredict() {
      return !!(this.form.title && this.form.startTime && this.form.endTime)
    },
    levelTagType() {
      if (!this.prediction) return 'info'
      return this.prediction.level === '高' ? 'success'
        : this.prediction.level === '低' ? 'danger' : 'warning'
    },
    levelColor() {
      if (!this.prediction) return '#909399'
      return this.prediction.level === '高' ? '#67c23a'
        : this.prediction.level === '低' ? '#f56c6c' : '#e6a23c'
    },
    // 当前进行中页内的置顶待办
    pendingTopRecords() {
      return this.pendingRecords.filter(t => t.top)
    },
    // 当前进行中页内的未置顶待办
    pendingNormalRecords() {
      return this.pendingRecords.filter(t => !t.top)
    },
    labelOptions() {
      const options = new Set(DEFAULT_TAGS)
      this.labelStats.forEach(item => options.add(item.name))
      if (this.form.label && !options.has(this.form.label)) {
        options.add(this.form.label)
      }
      return Array.from(options)
    },
    emptyDescription() {
      const filtered = this.searchKeyword || this.selectedLabels.length < this.labelStats.length
      return filtered ? '没有符合条件的待办' : '暂无待办，点击右上角新建'
    },
    doneEmptyDescription() {
      const filtered = this.searchKeyword || this.selectedLabels.length < this.labelStats.length
      return filtered ? '没有符合条件的已完成待办' : '暂无已完成的待办'
    }
  },
  watch: {
    // 搜索关键字防抖 300ms 后回到第 1 页重新查询
    searchKeyword() {
      this.reloadDebounced()
    },
    // 勾选标签变化立即重新查询
    selectedLabels() {
      this.pendingPageNum = 1
      this.donePageNum = 1
      this.doneLoaded = false
      this.fetchActivePage()
    }
  },
  created() {
    this.reloadDebounced = debounce(() => {
      this.pendingPageNum = 1
      this.donePageNum = 1
      this.fetchActivePage()
    }, 300)
    this.fetchCounts()
    this.fetchPage('pending')
    this.fetchLabelStats()
  },
  beforeUnmount() {
    this.reloadDebounced.cancel()
  },
  methods: {
    formatMinute,

    // ===== 查询 =====
    /** 构造分页查询参数（关键字 + 未勾选标签下沉后端排除） */
    buildPageParams(status) {
      const params = {
        status,
        pageNum: status === 'done' ? this.donePageNum : this.pendingPageNum,
        pageSize: this.pageSize
      }
      if (this.searchKeyword) params.keyword = this.searchKeyword
      const excluded = this.labelStats
        .map(item => item.name)
        .filter(name => !this.selectedLabels.includes(name))
      if (excluded.length) params.excludeLabel = excluded
      return params
    },

    /** 拉取分页待办：status=pending（进行中）/ done（已完成） */
    async fetchPage(status) {
      const isDone = status === 'done'
      this.loading = true
      try {
        const data = await todoApi.page(this.buildPageParams(status))
        const records = data.records || []
        if (isDone) {
          this.doneRecords = records
          this.doneTotal = data.total || 0
          this.doneLoaded = true
          // 删除最后一条导致空页时自动回退一页
          if (!records.length && this.doneTotal > 0 && this.donePageNum > 1) {
            this.donePageNum -= 1
            return this.fetchPage('done')
          }
        } else {
          this.pendingRecords = records
          this.pendingTotal = data.total || 0
          if (!records.length && this.pendingTotal > 0 && this.pendingPageNum > 1) {
            this.pendingPageNum -= 1
            return this.fetchPage('pending')
          }
        }
      } catch (err) {
        notifyError(err, '加载待办失败')
      } finally {
        this.loading = false
      }
    },

    /** 拉取当前 Tab 对应的页 */
    fetchActivePage() {
      if (this.activeTab === 'done') this.fetchPage('done')
      else if (this.activeTab === 'todos') this.fetchPage('pending')
    },

    /** 轻量计数（Tab 角标） */
    async fetchCounts() {
      try {
        this.counts = (await todoApi.counts()) || this.counts
      } catch (err) {
        // 计数失败不影响主流程
      }
    },

    async fetchLabelStats() {
      try {
        this.labelStats = await loadLabelStats()
      } catch (err) {
        // 标签统计失败不阻断主流程
      }
    },

    onPendingPageChange(page) {
      this.pendingPageNum = page
      this.fetchPage('pending')
    },
    onDonePageChange(page) {
      this.donePageNum = page
      this.fetchPage('done')
    },
    onPageSizeChange(size) {
      this.pageSize = size
      this.pendingPageNum = 1
      this.donePageNum = 1
      this.fetchActivePage()
    },

    /** 切换 Tab：回收站懒加载，已完成进入时加载 */
    onTabChange(tab) {
      if (tab === 'trash') {
        if (!this.trashLoaded) this.fetchTrash()
      } else if (tab === 'done') {
        this.fetchPage('done')
      } else if (tab === 'todos') {
        this.fetchPage('pending')
      }
    },

    /** 数据变更后统一刷新：当前列表 + 计数 + 标签统计 +（已加载的）回收站与日历/时间轴 */
    reloadAfterChange() {
      this.fetchCounts()
      this.fetchPage('pending')
      if (this.doneLoaded) this.fetchPage('done')
      if (this.trashLoaded) this.fetchTrash()
      this.fetchLabelStats()
      if (this.$refs.calendar) this.$refs.calendar.refresh()
      if (this.$refs.timeline) this.$refs.timeline.refresh()
    },

    // ===== 视图切换 =====
    toggleCalendarView() {
      this.viewMode = this.viewMode === 'calendar' ? 'list' : 'calendar'
    },
    toggleTimelineView() {
      this.viewMode = this.viewMode === 'timeline' ? 'list' : 'timeline'
    },

    // ===== 完成状态 =====
    async toggleComplete(todo) {
      const next = !todo.completed
      try {
        const updated = await todoApi.setCompleted(todo.id, next)
        this.$message.success(next ? '已标记为完成' : '已取消完成')
        // 详情弹窗打开时同步刷新详情数据
        if (this.detailVisible && this.detail && this.detail.id === todo.id && updated) {
          this.detail = updated
        }
        this.reloadAfterChange()
      } catch (err) {
        notifyError(err, '操作失败')
      }
    },
    toggleCompleteFromDetail() {
      if (this.detail) {
        this.toggleComplete(this.detail)
      }
    },

    // ===== 详情 / 表单 =====
    async openDetail(todo) {
      try {
        this.detail = await todoApi.detail(todo.id)
        this.detailVisible = true
      } catch (err) {
        notifyError(err, '加载详情失败')
      }
    },
    editFromDetail() {
      const todo = this.detail
      this.detailVisible = false
      this.openEdit(todo)
    },
    openCreate() {
      this.form = defaultForm()
      this.prediction = null
      this.dialogVisible = true
    },
    openEdit(todo) {
      this.prediction = null
      this.form = {
        id: todo.id,
        title: todo.title || '',
        content: todo.content || '',
        label: todo.label || '',
        startTime: todo.startTime || '',
        endTime: todo.endTime || '',
        remindTime: todo.remindTime || '',
        daily: !!todo.daily,
        repeatUntil: todo.repeatUntil || '',
        top: !!todo.top
      }
      this.dialogVisible = true
    },
    handleDailyChange(value) {
      if (!value) this.form.repeatUntil = ''
    },
    resetForm() {
      this.form = defaultForm()
    },
    repeatUntilDisabled(date) {
      if (!this.form.startTime) return false
      const start = parseLocal(this.form.startTime)
      const dayStart = new Date(start.getFullYear(), start.getMonth(), start.getDate())
      return date.getTime() < dayStart.getTime()
    },
    async saveTodo() {
      if (!this.form.title) {
        this.$message.warning('请填写待办标题')
        return
      }
      if (!this.form.startTime || !this.form.endTime) {
        this.$message.warning('请填写开始时间和结束时间')
        return
      }
      const start = parseLocal(this.form.startTime)
      const end = parseLocal(this.form.endTime)
      if (end.getTime() <= start.getTime()) {
        this.$message.warning('结束时间必须晚于开始时间')
        return
      }
      if (this.form.daily && this.form.repeatUntil) {
        const repeatEnd = parseLocal(this.form.repeatUntil + ' 00:00')
        const startDate = new Date(start.getFullYear(), start.getMonth(), start.getDate())
        if (repeatEnd < startDate) {
          this.$message.warning('每日重复截止日期不能早于开始日期')
          return
        }
      }

      const payload = {
        id: this.form.id,
        title: this.form.title,
        content: this.form.content || '',
        label: this.form.label || null,
        startTime: this.form.startTime,
        endTime: this.form.endTime,
        remindTime: this.form.remindTime || null,
        daily: this.form.daily,
        repeatUntil: this.form.daily ? this.form.repeatUntil || null : null,
        top: this.form.top
      }

      this.saving = true
      try {
        const saved = await todoApi.save(payload)
        this.$message.success(this.form.id ? '修改成功' : '创建成功')
        this.dialogVisible = false
        this.reloadAfterChange()
        return saved
      } catch (err) {
        notifyError(err, '保存失败')
      } finally {
        this.saving = false
      }
    },

    // ===== 删除 / 回收站 =====
    removeTodo(todo) {
      this.$confirm(`确定删除待办"${todo.title}"吗？删除后可在回收站找回。`, '删除确认', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      })
        .then(async () => {
          try {
            await todoApi.remove(todo.id)
            this.$message.success('已移入回收站')
            this.reloadAfterChange()
            if (this.trashLoaded) this.fetchTrash()
          } catch (err) {
            notifyError(err, '删除失败')
          }
        })
        .catch(() => {})
    },
    async fetchTrash() {
      this.trashLoading = true
      try {
        this.trashTodos = (await todoApi.trash()) || []
        this.trashLoaded = true
      } catch (err) {
        // 忽略 401（拦截器统一处理）
      } finally {
        this.trashLoading = false
      }
    },
    async restoreTodo(todo) {
      try {
        await todoApi.restore(todo.id)
        this.$message.success('恢复成功')
        this.fetchTrash()
        this.reloadAfterChange()
      } catch (err) {
        notifyError(err, '恢复失败')
      }
    },
    permanentlyDeleteTodo(todo) {
      this.$confirm(`确定彻底删除待办"${todo.title}"吗？此操作不可恢复！`, '危险操作', {
        type: 'error',
        confirmButtonText: '彻底删除',
        cancelButtonText: '取消'
      })
        .then(async () => {
          try {
            await todoApi.permanentRemove(todo.id)
            this.$message.success('已彻底删除')
            this.fetchTrash()
          } catch (err) {
            notifyError(err, '删除失败')
          }
        })
        .catch(() => {})
    },

    // ===== 导出 / AI =====
    async exportCsv() {
      this.exporting = true
      try {
        await downloadBlob(todoApi.exportFile(), '待办导出.csv')
        this.$message.success('待办已导出为 CSV 文件')
      } catch (err) {
        notifyError(err, '导出失败，请稍后重试')
      } finally {
        this.exporting = false
      }
    },

    /** AI 预测当前新任务的完成成功率 */
    async predictSuccessRate() {
      if (!this.canPredict) {
        this.$message.warning('请先填写标题和开始/结束时间')
        return
      }
      this.predicting = true
      try {
        this.prediction = await aiApi.predict({
          title: this.form.title,
          content: this.form.content || '',
          label: this.form.label || null,
          startTime: this.form.startTime,
          endTime: this.form.endTime,
          daily: this.form.daily,
          top: this.form.top
        })
      } catch (err) {
        notifyError(err, '预测失败，请稍后重试')
      } finally {
        this.predicting = false
      }
    },

    /** 状态 -> 标签颜色：已完成绿色、逾期红色、进行中蓝色 */
    statusType(status) {
      if (status === '已完成') return 'success'
      if (status === '逾期') return 'danger'
      return 'primary'
    }
  }
}
</script>

<style scoped>
.todo-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 22px 20px 40px;
  text-align: left;
}
.page-heading {
  margin: 0 0 16px;
  font-size: 22px;
  font-weight: 700;
  color: var(--wb-text-primary);
  display: flex;
  align-items: center;
  gap: 10px;
}
.page-heading::before {
  content: '';
  width: 5px;
  height: 22px;
  border-radius: 3px;
  background: var(--wb-gradient-brand);
  display: inline-block;
}
.page-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 18px;
  background: #fff;
  border: 1px solid var(--wb-border);
  border-radius: 12px;
  padding: 12px 16px;
  box-shadow: var(--wb-card-shadow);
}
.toolbar-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.todo-group {
  margin-bottom: 26px;
}
.group-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 12px;
}
.pagination-bar {
  display: flex;
  justify-content: center;
  margin: 24px 0 8px;
}
/* 详情弹窗 */
.todo-detail {
  text-align: left;
}
.detail-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 700;
  word-break: break-word;
}
.detail-desc {
  margin-bottom: 8px;
}
.detail-content {
  padding: 10px 12px;
  border-left: 3px solid var(--el-border-color);
  background: var(--el-fill-color-light);
  border-radius: 0 4px 4px 0;
  line-height: 1.7;
  max-height: 40vh;
  overflow-y: auto;
}
.detail-content :deep(img) {
  max-width: 100%;
}
.done-title {
  text-decoration: line-through;
  color: var(--el-text-color-placeholder);
}
.overdue-text {
  color: var(--el-color-danger);
  font-weight: 600;
}
.label-tip {
  width: 100%;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  line-height: 1.4;
}
/* AI 完成率预测 */
.predict-box {
  margin-top: 12px;
  padding: 12px 14px;
  background: #fdf6ec;
  border: 1px solid #f5dab1;
  border-radius: 6px;
  width: 100%;
}
.predict-rate {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.rate-num {
  font-size: 22px;
  font-weight: 700;
}
.rate-num.lv-高 { color: #67c23a; }
.rate-num.lv-中 { color: #e6a23c; }
.rate-num.lv-低 { color: #f56c6c; }
.predict-reason {
  margin-top: 8px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}
</style>
