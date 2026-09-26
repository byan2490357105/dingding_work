<template>
  <div class="notes-page">
    <div class="notes-body">
      <!-- 工具栏：新建笔记 + 导出 -->
      <div class="notes-toolbar">
        <h2 class="page-title">我的随手记</h2>
        <div class="toolbar-actions">
          <el-input
            v-model="searchKeyword"
            placeholder="按标题/关键字搜索笔记"
            clearable
            style="width: 220px"
            :prefix-icon="Search"
          />
          <el-button size="large" @click="openWordCloud">
            <el-icon><Histogram /></el-icon>&nbsp;词云
          </el-button>
          <el-button size="large" :loading="exportingCsv" @click="exportCsv">
            <el-icon><Download /></el-icon>&nbsp;导出CSV
          </el-button>
          <el-button size="large" type="success" plain :loading="exportingWord" @click="exportWord">
            <el-icon><Document /></el-icon>&nbsp;导出Word
          </el-button>
          <el-button size="large" type="warning" :loading="generatingReport" @click="generateNoteReport">
            <el-icon><MagicStick /></el-icon>&nbsp;AI 月报
          </el-button>
          <el-button size="large" type="warning" plain :loading="generatingReport" @click="generateNoteWeeklyReport">
            <el-icon><MagicStick /></el-icon>&nbsp;AI 周报
          </el-button>
          <el-button type="primary" size="large" @click="openCreate">
            <el-icon><EditPen /></el-icon>&nbsp;新建笔记
          </el-button>
        </div>
      </div>

      <!-- 标签筛选面板（可折叠） -->
      <div class="tag-filter-panel" v-if="allTags.length > 0">
        <div class="tag-filter-head" @click="tagPanelCollapsed = !tagPanelCollapsed">
          <el-icon class="collapse-arrow" :class="{ collapsed: tagPanelCollapsed }"><ArrowDown /></el-icon>
          <span class="tag-filter-title">按标签筛选（勾选显示，取消勾选则隐藏该标签的笔记）</span>
          <el-tag size="small" type="info">{{ selectedTags.length }}/{{ allTags.length }} 个标签</el-tag>
        </div>
        <div class="tag-filter-body" v-show="!tagPanelCollapsed">
          <div class="tag-filter-toolbar">
            <el-input
              v-model="tagSearchKey"
              placeholder="搜索标签"
              size="small"
              clearable
              :prefix-icon="Search"
              style="width: 200px"
            />
            <el-button size="small" link @click="selectedTags = allTags.slice()">全选</el-button>
            <el-button size="small" link @click="selectedTags = []">全不选</el-button>
          </div>
          <div class="tag-carousel">
            <el-button
              v-if="totalTagPages > 1"
              link
              :disabled="tagPage === 0"
              class="tag-nav-btn"
              @click="prevTagPage"
            >
              <el-icon><ArrowLeft /></el-icon>
            </el-button>
            <transition :name="tagSlideDir === 'next' ? 'tag-slide-next' : 'tag-slide-prev'" mode="out-in">
              <div :key="tagPage" class="tag-page">
                <el-checkbox-group v-model="selectedTags" class="tag-checkbox-row">
                  <el-checkbox
                    v-for="tag in pagedTags"
                    :key="tag"
                    :label="tag"
                    class="tag-checkbox-item"
                  >
                    <el-tag size="small" effect="plain">{{ tag }}</el-tag>
                    <span class="tag-count">{{ tagCount(tag) }}</span>
                  </el-checkbox>
                </el-checkbox-group>
              </div>
            </transition>
            <el-button
              v-if="totalTagPages > 1"
              link
              :disabled="tagPage >= totalTagPages - 1"
              class="tag-nav-btn"
              @click="nextTagPage"
            >
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="notes-tabs" @tab-change="onTabChange">
        <el-tab-pane label="我的笔记" name="notes">
          <!-- 置顶笔记（置顶排序在最前，翻到后续页无置顶时整块隐藏） -->
          <div class="section" v-if="filteredPinnedNotes.length > 0">
            <h3 class="section-title">
              <el-icon><Top /></el-icon> 置顶笔记
              <el-tag size="small" type="warning" round>{{ filteredPinnedNotes.length }}</el-tag>
            </h3>
            <el-row :gutter="16">
              <el-col v-for="note in filteredPinnedNotes" :key="note.id" :span="8">
                <note-card
                  :note="note"
                  @view="openView"
                  @edit="openEdit"
                  @toggle-pin="togglePin"
                  @remove="removeNote"
                />
              </el-col>
            </el-row>
          </div>

          <!-- 未置顶笔记 -->
          <div class="section">
            <h3 class="section-title">
              <el-icon><Document /></el-icon> 全部笔记
              <el-tag size="small" type="info" round>{{ filteredNormalNotes.length }}</el-tag>
            </h3>
            <el-empty v-if="filteredNormalNotes.length === 0" :description="searchKeyword || selectedTags.length < allTags.length ? '没有符合条件的笔记' : '还没有笔记，点击上方新建笔记开始记录吧'" :image-size="60" />
            <el-row :gutter="16">
              <el-col v-for="note in filteredNormalNotes" :key="note.id" :span="8">
                <note-card
                  :note="note"
                  @view="openView"
                  @edit="openEdit"
                  @toggle-pin="togglePin"
                  @remove="removeNote"
                />
              </el-col>
            </el-row>
          </div>

          <!-- 分页 -->
          <div class="pagination-bar" v-if="pageTotal > 0">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next, jumper"
              :total="pageTotal"
              :current-page="pageNum"
              :page-size="pageSize"
              :page-sizes="[9, 12, 24, 48]"
              @current-change="onPageChange"
              @size-change="onSizeChange"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane :label="`回收站(${trashNotes.length})`" name="trash">
          <el-empty v-if="trashNotes.length === 0" description="回收站是空的" :image-size="80" />
          <el-row v-else :gutter="16">
            <el-col v-for="note in trashNotes" :key="note.id" :span="8">
              <div class="trash-card">
                <div class="trash-card-head">
                  <span class="trash-title">{{ note.title }}</span>
                  <el-tag v-if="note.tag" size="small" type="info" effect="plain">{{ note.tag }}</el-tag>
                </div>
                <div class="trash-card-content" v-html="note.content"></div>
                <div class="trash-card-time">修改于 {{ formatDetailTime(note.updateTime) }}</div>
                <div class="trash-card-actions">
                  <el-button size="small" type="success" plain @click="restoreNote(note)">恢复</el-button>
                  <el-button size="small" type="danger" plain @click="permanentlyDeleteNote(note)">彻底删除</el-button>
                </div>
              </div>
            </el-col>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 新建 / 编辑 弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑笔记' : '新建笔记'"
      width="640px"
      :close-on-click-modal="false"
      @closed="onDialogClosed"
    >
      <el-form :model="form" label-width="70px">
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="200" placeholder="请输入笔记标题" />
        </el-form-item>

        <el-form-item label="标签">
          <el-select
            v-model="form.tag"
            placeholder="选择或输入自定义标签"
            filterable
            allow-create
            default-first-option
            style="width: 100%"
          >
            <el-option v-for="t in tagSelectOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>

        <el-form-item label="内容">
          <div class="rich-editor">
            <div class="rich-toolbar">
              <el-button size="small" @mousedown.prevent="exec('bold')"><b>B</b></el-button>
              <el-button size="small" @mousedown.prevent="exec('italic')"><i>I</i></el-button>
              <el-button size="small" @mousedown.prevent="exec('underline')"><u>U</u></el-button>
              <el-button size="small" @mousedown.prevent="exec('strikeThrough')"><s>S</s></el-button>
              <el-button size="small" @mousedown.prevent="exec('insertUnorderedList')">• 列表</el-button>
              <el-button size="small" @mousedown.prevent="exec('insertOrderedList')">1. 列表</el-button>
              <el-button size="small" @mousedown.prevent="exec('removeFormat')">清除格式</el-button>
            </div>
            <div
              ref="editorRef"
              class="rich-content"
              contenteditable="true"
              data-placeholder="记录点什么..."
            ></div>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveNote">保存</el-button>
      </template>
    </el-dialog>

    <!-- 笔记详情弹窗 -->
    <el-dialog v-model="detailVisible" title="笔记详情" width="680px">
      <div v-if="detail" class="note-detail">
        <h2 class="detail-title">{{ detail.title }}</h2>
        <div class="detail-meta">
          <el-tag v-if="detail.tag" size="small" type="success" effect="plain">{{ detail.tag }}</el-tag>
          <el-tag v-if="detail.isPinned === 1" size="small" type="warning" effect="plain">已置顶</el-tag>
          <span>创建：{{ formatDetailTime(detail.createTime) }}</span>
          <span>修改：{{ formatDetailTime(detail.updateTime) }}</span>
        </div>
        <el-divider />
        <div class="detail-content" v-html="detail.content || '<span style=\'color:#909399\'>（无内容）</span>'"></div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="editFromDetail">编辑</el-button>
      </template>
    </el-dialog>

    <!-- AI 笔记月报 / 周报弹窗 -->
    <el-dialog
      v-model="reportVisible"
      :title="reportTitle"
      width="720px"
      :close-on-click-modal="false"
    >
      <div v-loading="generatingReport">
        <div class="report-period">统计周期：{{ reportPeriod }}</div>
        <div v-if="reportCached" class="report-cached-tip">（来自缓存，数据未更新）</div>
        <div class="report-content" v-html="reportHtml"></div>
      </div>
      <template #footer>
        <el-button type="primary" plain @click="openHistory">历史报告</el-button>
        <el-button @click="reportVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- AI 报告历史抽屉 -->
    <el-drawer
      v-model="historyVisible"
      title="AI 报告历史"
      size="460px"
      :append-to-body="true"
    >
      <div class="history-toolbar">
        <el-radio-group v-model="historyType" @change="loadHistory">
          <el-radio-button label="note-monthly">月报</el-radio-button>
          <el-radio-button label="note-weekly">周报</el-radio-button>
        </el-radio-group>
        <el-button size="small" @click="loadHistory">刷新</el-button>
      </div>
      <div v-loading="historyLoading" class="history-body">
        <el-empty v-if="historyList.length === 0" description="暂无历史报告" :image-size="60" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="rep in historyList"
            :key="rep.id"
            :timestamp="formatHistoryTime(rep.generatedAt)"
            placement="top"
          >
            <el-card
              shadow="hover"
              class="history-card"
              @click="showHistoryReport(rep)"
            >
              <div class="history-period">{{ rep.period }}</div>
              <div class="history-type">类型：{{ reportTypeLabel(rep.reportType) }}</div>
              <div class="history-click-tip">点击查看完整报告</div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>

    <!-- 词云弹窗 -->
    <el-dialog v-model="wordCloudVisible" title="笔记词云" width="640px" top="10vh">
      <el-empty v-if="wordCloudWords.length === 0" description="暂无笔记内容生成词云" :image-size="80" />
      <div v-else class="word-cloud">
        <span
          v-for="w in wordCloudWords"
          :key="w.name"
          class="wc-word"
          :style="{ fontSize: wordSize(w.value, wordMax()) + 'px', color: wcColor(w.value, wordMax()) }"
          :title="'出现 ' + w.value + ' 次'"
        >{{ w.name }}</span>
      </div>
      <div class="wc-tip">基于所有笔记标题与正文的高频词生成，字号越大出现频次越高</div>
    </el-dialog>
  </div>
</template>

<script>
import { Top, Document, EditPen, Download, MagicStick, Search, ArrowDown, ArrowLeft, ArrowRight, Histogram } from '@element-plus/icons-vue'
import NoteCard from '../components/NoteCard.vue'
import { renderMarkdown } from '../utils/markdown'

export default {
  name: 'Notes',
  components: { Top, Document, EditPen, Download, MagicStick, Search, ArrowDown, ArrowLeft, ArrowRight, Histogram, NoteCard },
  data() {
    return {
      // 默认标签
      defaultTags: ['学习', '生活', '科研', '出行'],
      activeTab: 'notes',
      // 分页数据（后端分页）
      pageRecords: [],
      pageNum: 1,
      pageSize: 9,
      pageTotal: 0,
      tagStats: [],
      trashNotes: [],
      exportingCsv: false,
      exportingWord: false,
      generatingReport: false,
      reportVisible: false,
      reportTitle: 'AI 笔记月报',
      reportPeriod: '',
      reportHtml: '',
      reportCached: false,
      historyVisible: false,
      historyLoading: false,
      historyList: [],
      historyType: 'note-monthly',
      dialogVisible: false,
      detailVisible: false,
      detail: null,
      saving: false,
      // 标签筛选 / 搜索 / 词云
      searchKeyword: '',
      selectedTags: [],
      tagSearchKey: '',
      tagPage: 0,
      tagSlideDir: 'next',
      tagPanelCollapsed: false,
      wordCloudVisible: false,
      wordCloudWords: [],
      form: {
        id: null,
        title: '',
        content: '',
        tag: ''
      }
    }
  },
  created() {
    this.fetchPage()
    this.fetchTagStats()
    this.fetchTrash()
  },
  watch: {
    // 搜索关键字防抖 300ms 后重新查询并回到第 1 页
    searchKeyword() {
      this.scheduleReload()
    },
    // 勾选标签变化立即重新查询
    selectedTags() {
      this.pageNum = 1
      this.fetchPage()
    },
    // 标签统计变化：首次全选，后续只勾选新出现的标签
    tagStats(newStats, oldStats) {
      if (!oldStats || oldStats.length === 0) {
        // 首次加载：默认全选
        this.selectedTags = this.allTags.slice()
      } else {
        // 后续刷新：只自动勾选新出现的标签
        const oldTags = oldStats.map(t => t.tag)
        const newTags = this.allTags.filter(t => !oldTags.includes(t))
        if (newTags.length) {
          this.selectedTags = [...this.selectedTags, ...newTags]
        }
      }
    },
    // 搜索标签时重置到第一页
    tagSearchKey() {
      this.tagPage = 0
    }
  },
  computed: {
    // 所有不重复标签（来自后端全量统计，含自定义标签）
    allTags() {
      return this.tagStats.map(t => t.tag)
    },
    // 标签面板按搜索关键字过滤后的显示列表
    visibleTags() {
      if (!this.tagSearchKey) return this.allTags
      const kw = this.tagSearchKey.toLowerCase()
      return this.allTags.filter(t => t.toLowerCase().includes(kw))
    },
    // 标签总页数（每页 5 个）
    totalTagPages() {
      return Math.max(1, Math.ceil(this.visibleTags.length / 5))
    },
    // 当前页显示的标签（5 个一组）
    pagedTags() {
      const start = this.tagPage * 5
      return this.visibleTags.slice(start, start + 5)
    },
    // 标签下拉选项：默认标签 + 后端已有标签（含自定义），去重
    tagSelectOptions() {
      const set = new Set(this.defaultTags)
      this.allTags.forEach(t => set.add(t))
      if (this.form.tag && !set.has(this.form.tag)) set.add(this.form.tag)
      return Array.from(set)
    },
    // 当前页内的置顶笔记
    filteredPinnedNotes() {
      return this.pageRecords.filter(n => n.isPinned === 1)
    },
    // 当前页内的未置顶笔记
    filteredNormalNotes() {
      return this.pageRecords.filter(n => n.isPinned !== 1)
    }
  },
  methods: {
    // 统计某标签下的笔记数（全量，来自后端）
    tagCount(tag) {
      const item = this.tagStats.find(t => t.tag === tag)
      return item ? item.count : 0
    },
    // 搜索输入防抖
    scheduleReload() {
      clearTimeout(this._searchTimer)
      this._searchTimer = setTimeout(() => {
        this.pageNum = 1
        this.fetchPage()
      }, 300)
    },
    // 拉取分页笔记（关键字 / 未勾选标签下沉后端排除）
    async fetchPage() {
      try {
        const params = {
          pageNum: this.pageNum,
          pageSize: this.pageSize
        }
        if (this.searchKeyword) params.keyword = this.searchKeyword
        // 未勾选的标签 = 全部标签 - 已勾选标签
        const excluded = this.allTags.filter(t => !this.selectedTags.includes(t))
        if (excluded.length) params.excludeTag = excluded
        const res = await this.$http.get('/api/notes/page', { params })
        if (res.data.code === 200) {
          this.pageRecords = res.data.data.records || []
          this.pageTotal = res.data.data.total || 0
        }
      } catch (err) {
        if (!err.response || err.response.status !== 401) {
          this.$message.error('加载笔记失败')
        }
      }
    },
    // 拉取全量标签统计
    async fetchTagStats() {
      try {
        const res = await this.$http.get('/api/notes/tags')
        if (res.data.code === 200) {
          this.tagStats = res.data.data || []
        }
      } catch (err) {
        // 标签统计失败不阻断主流程
      }
    },
    // 分页：页码变化
    onPageChange(page) {
      this.pageNum = page
      this.fetchPage()
    },
    // 分页：每页条数变化
    onSizeChange(size) {
      this.pageSize = size
      this.pageNum = 1
      this.fetchPage()
    },
    // 标签轮播：上一页
    prevTagPage() {
      if (this.tagPage > 0) {
        this.tagSlideDir = 'prev'
        this.tagPage--
      }
    },
    // 标签轮播：下一页
    nextTagPage() {
      if (this.tagPage < this.totalTagPages - 1) {
        this.tagSlideDir = 'next'
        this.tagPage++
      }
    },
    // 去除 HTML 标签（词云已在后端处理，此方法保留供其他展示复用）
    stripHtml(html) {
      const div = document.createElement('div')
      div.innerHTML = html
      return div.textContent || div.innerText || ''
    },
    // 打开词云弹窗：数据由后端全量统计
    async openWordCloud() {
      try {
        const res = await this.$http.get('/api/notes/wordcloud', { params: { limit: 50 } })
        if (res.data.code === 200) {
          this.wordCloudWords = res.data.data || []
          this.wordCloudVisible = true
        }
      } catch (err) {
        this.$message.error('词云生成失败')
      }
    },
    // 词云词的字号：频率越高字号越大（12px ~ 36px）
    wordSize(value, max) {
      if (!max) return 14
      return Math.round(12 + (value / max) * 24)
    },
    wordMax() {
      return this.wordCloudWords.length ? this.wordCloudWords[0].value : 1
    },
    // 词云颜色：高频偏暖色，低频偏冷色
    wcColor(value, max) {
      const ratio = max ? value / max : 0
      if (ratio > 0.66) return '#e6a23c'
      if (ratio > 0.33) return '#409eff'
      return '#909399'
    },
    // 切换 Tab 时刷新回收站
    onTabChange(tab) {
      if (tab === 'trash') {
        this.fetchTrash()
      }
    },

    // 通用文件下载：blob 方式请求（自动携带 Token），从响应头解析文件名
    async downloadFile(url, fallbackName, tip) {
      const res = await this.$http.get(url, { responseType: 'blob' })
      const disposition = res.headers['content-disposition'] || ''
      let filename = fallbackName
      const match = disposition.match(/filename\*=UTF-8''([^;]+)/i)
      if (match) {
        filename = decodeURIComponent(match[1])
      }
      const link = document.createElement('a')
      link.href = URL.createObjectURL(res.data)
      link.download = filename
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(link.href)
      this.$message.success(tip)
    },
    // 导出笔记 CSV
    async exportCsv() {
      this.exportingCsv = true
      try {
        await this.downloadFile('/api/notes/export/csv', '笔记导出.csv', '笔记已导出为 CSV 文件')
      } catch (err) {
        if (!err.response || err.response.status !== 401) {
          this.$message.error('导出失败，请稍后重试')
        }
      } finally {
        this.exportingCsv = false
      }
    },
    // 导出笔记 Word
    async exportWord() {
      this.exportingWord = true
      try {
        await this.downloadFile('/api/notes/export/word', '随手记.docx', '笔记已导出为 Word 文档')
      } catch (err) {
        if (!err.response || err.response.status !== 401) {
          this.$message.error('导出失败，请稍后重试')
        }
      } finally {
        this.exportingWord = false
      }
    },

    // 生成 AI 笔记月报
    async generateNoteReport() {
      this.reportTitle = 'AI 笔记月报'
      this.reportVisible = true
      this.generatingReport = true
      this.reportCached = false
      this.reportHtml = '<div style="color:#909399">AI 正在分析你近一个月的笔记，请稍候...</div>'
      this.reportPeriod = ''
      try {
        const res = await this.$http.get('/api/ai/report/notes')
        if (res.data.code === 200) {
          this.reportPeriod = res.data.data.period
          this.reportCached = !!res.data.data.cached
          this.reportHtml = renderMarkdown(res.data.data.content)
        } else {
          this.reportHtml = '<div style="color:#f56c6c">' + (res.data.message || '生成失败') + '</div>'
        }
      } catch (err) {
        const msg = (err.response && err.response.data && err.response.data.message) || '生成失败，请稍后重试'
        this.reportHtml = '<div style="color:#f56c6c">' + msg + '</div>'
      } finally {
        this.generatingReport = false
      }
    },

    // 生成 AI 笔记周报
    async generateNoteWeeklyReport() {
      this.reportTitle = 'AI 笔记周报'
      this.reportVisible = true
      this.generatingReport = true
      this.reportCached = false
      this.reportHtml = '<div style="color:#909399">AI 正在分析你近一周的笔记，请稍候...</div>'
      this.reportPeriod = ''
      try {
        const res = await this.$http.get('/api/ai/report/notes/weekly')
        if (res.data.code === 200) {
          this.reportPeriod = res.data.data.period
          this.reportCached = !!res.data.data.cached
          this.reportHtml = renderMarkdown(res.data.data.content)
        } else {
          this.reportHtml = '<div style="color:#f56c6c">' + (res.data.message || '生成失败') + '</div>'
        }
      } catch (err) {
        const msg = (err.response && err.response.data && err.response.data.message) || '生成失败，请稍后重试'
        this.reportHtml = '<div style="color:#f56c6c">' + msg + '</div>'
      } finally {
        this.generatingReport = false
      }
    },

    // 打开历史报告抽屉：默认显示当前报告类型
    openHistory() {
      const isWeekly = (this.reportTitle || '').includes('周报')
      this.historyType = isWeekly ? 'note-weekly' : 'note-monthly'
      this.historyVisible = true
      this.loadHistory()
    },
    // 拉取历史报告列表
    async loadHistory() {
      this.historyLoading = true
      try {
        const res = await this.$http.get('/api/ai/report/history', {
          params: { type: this.historyType, limit: 12 }
        })
        if (res.data.code === 200) {
          this.historyList = res.data.data || []
        } else {
          this.$message.error(res.data.message || '加载历史失败')
        }
      } catch (err) {
        if (!err.response || err.response.status !== 401) {
          this.$message.error('加载历史失败')
        }
      } finally {
        this.historyLoading = false
      }
    },
    // 点击历史报告：填充到主弹窗展示
    showHistoryReport(rep) {
      this.reportTitle = 'AI 笔记' + (rep.reportType === 'note-weekly' ? '周报' : '月报') + '（历史）'
      this.reportPeriod = rep.period || ''
      this.reportCached = false
      this.reportHtml = renderMarkdown(rep.content)
      this.historyVisible = false
      this.reportVisible = true
    },
    // 历史报告时间格式化（兼容 ISO 字符串与数组）
    formatHistoryTime(t) {
      if (!t) return ''
      if (Array.isArray(t)) {
        const [y, m, d, hh = 0, mm = 0] = t
        return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(hh).padStart(2, '0')}:${String(mm).padStart(2, '0')}`
      }
      return String(t).replace('T', ' ').slice(0, 16)
    },
    reportTypeLabel(type) {
      if (type === 'note-weekly') return '笔记周报'
      if (type === 'note-monthly') return '笔记月报'
      if (type === 'todo-weekly') return '待办周报'
      if (type === 'todo-monthly') return '待办月报'
      return type || '未知'
    },

    // 查看笔记详情：请求后端获取最新详情
    async openView(note) {
      try {
        const res = await this.$http.get(`/api/notes/${note.id}`)
        if (res.data.code === 200) {
          this.detail = res.data.data
          this.detailVisible = true
        } else {
          this.$message.error(res.data.message || '加载详情失败')
        }
      } catch (err) {
        if (!err.response || err.response.status !== 401) {
          this.$message.error('加载详情失败')
        }
      }
    },

    // 从详情弹窗直接进入编辑
    editFromDetail() {
      const note = this.detail
      this.detailVisible = false
      this.openEdit(note)
    },

    // 详情时间格式化（兼容 ISO 字符串与数组）
    formatDetailTime(t) {
      if (!t) return ''
      if (Array.isArray(t)) {
        const [y, m, d, hh = 0, mm = 0] = t
        return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(hh).padStart(2, '0')}:${String(mm).padStart(2, '0')}`
      }
      return String(t).replace('T', ' ').slice(0, 16)
    },

    openCreate() {
      this.form = { id: null, title: '', content: '', tag: '' }
      this.dialogVisible = true
      // 等待弹窗渲染后清空富文本
      this.$nextTick(() => {
        if (this.$refs.editorRef) this.$refs.editorRef.innerHTML = ''
      })
    },

    openEdit(note) {
      this.form = {
        id: note.id,
        title: note.title,
        content: note.content || '',
        tag: note.tag || ''
      }
      this.dialogVisible = true
      // 回填富文本内容
      this.$nextTick(() => {
        if (this.$refs.editorRef) this.$refs.editorRef.innerHTML = note.content || ''
      })
    },

    // 富文本命令（@mousedown.prevent 已阻止按钮抢夺光标）
    exec(command) {
      document.execCommand(command, false, null)
      if (this.$refs.editorRef) this.$refs.editorRef.focus()
    },

    async saveNote() {
      if (!this.form.title.trim()) {
        this.$message.warning('请填写笔记标题')
        return
      }
      // 从富文本编辑器取出 HTML
      const html = this.$refs.editorRef ? this.$refs.editorRef.innerHTML : ''
      const payload = {
        title: this.form.title.trim(),
        content: html,
        tag: this.form.tag || ''
      }
      if (this.form.id) payload.id = this.form.id

      this.saving = true
      try {
        // 新建走 POST，修改走 PUT，路径相同
        const method = this.form.id ? 'put' : 'post'
        const res = await this.$http[method]('/api/notes', payload)
        if (res.data.code === 200) {
          this.$message.success(this.form.id ? '修改成功' : '新建成功')
          this.dialogVisible = false
          this.fetchPage()
          this.fetchTagStats()
        } else {
          this.$message.error(res.data.message || '保存失败')
        }
      } catch (err) {
        this.$message.error('保存失败')
      } finally {
        this.saving = false
      }
    },

    async togglePin(note) {
      try {
        const res = await this.$http.put(`/api/notes/pin/${note.id}`)
        if (res.data.code === 200) {
          this.$message.success(note.isPinned === 1 ? '已取消置顶' : '已置顶')
          this.fetchPage()
          this.fetchTagStats()
        }
      } catch (err) {
        this.$message.error('操作失败')
      }
    },

    removeNote(note) {
      this.$confirm('确定删除这条笔记吗？删除后可在回收站找回。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await this.$http.delete(`/api/notes/${note.id}`)
          if (res.data.code === 200) {
            this.$message.success('已移入回收站')
            this.fetchPage()
          this.fetchTagStats()
            this.fetchTrash()
          }
        } catch (err) {
          this.$message.error('删除失败')
        }
      }).catch(() => {})
    },

    // 拉取回收站笔记列表
    async fetchTrash() {
      try {
        const res = await this.$http.get('/api/notes/trash')
        if (res.data.code === 200) {
          this.trashNotes = res.data.data || []
        }
      } catch (err) {
        // 忽略 401（拦截器统一处理）
      }
    },

    // 从回收站恢复笔记
    async restoreNote(note) {
      try {
        const res = await this.$http.put(`/api/notes/restore/${note.id}`)
        if (res.data.code === 200) {
          this.$message.success('恢复成功')
          this.fetchTrash()
          this.fetchPage()
          this.fetchTagStats()
        } else {
          this.$message.error(res.data.message || '恢复失败')
        }
      } catch (err) {
        this.$message.error('恢复失败')
      }
    },

    // 彻底删除笔记（不可恢复）
    permanentlyDeleteNote(note) {
      this.$confirm(`确定彻底删除笔记"${note.title}"吗？此操作不可恢复！`, '危险操作', {
        confirmButtonText: '彻底删除',
        cancelButtonText: '取消',
        type: 'error'
      }).then(async () => {
        try {
          const res = await this.$http.delete(`/api/notes/permanent/${note.id}`)
          if (res.data.code === 200) {
            this.$message.success('已彻底删除')
            this.fetchTrash()
          }
        } catch (err) {
          this.$message.error('删除失败')
        }
      }).catch(() => {})
    },

    onDialogClosed() {
      if (this.$refs.editorRef) this.$refs.editorRef.innerHTML = ''
    }
  }
}
</script>

<style scoped>
.notes-page {
  text-align: left;
}
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
.report-content h2 {
  font-size: 17px;
  color: #2e74b5;
  margin: 18px 0 10px;
  padding-bottom: 6px;
  border-bottom: 2px solid #e4e7ed;
}
.report-content h3 {
  font-size: 15px;
  color: #409eff;
  margin: 14px 0 8px;
}
.report-content strong {
  color: #303133;
}
.report-content p {
  margin: 8px 0;
}
.report-content ul, .report-content ol {
  margin: 8px 0;
  padding-left: 24px;
}
.report-content li {
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
.notes-body {
  padding: 20px;
  max-width: 1100px;
  margin: 0 auto;
}
.notes-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 4px 0;
}
.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}
.section {
  margin-bottom: 30px;
}
.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
}
.rich-editor {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}
.note-detail {
  text-align: left;
}
.detail-title {
  margin: 0 0 12px;
  font-size: 20px;
  font-weight: 700;
  word-break: break-word;
}
.detail-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #909399;
}
.detail-content {
  max-height: 50vh;
  overflow-y: auto;
  line-height: 1.8;
  word-break: break-word;
}
.detail-content :deep(img) {
  max-width: 100%;
}
.detail-content :deep(ul),
.detail-content :deep(ol) {
  padding-left: 24px;
}
.rich-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding: 6px;
  background: #f5f7fa;
  border-bottom: 1px solid #dcdfe6;
}
.rich-content {
  min-height: 180px;
  max-height: 320px;
  overflow-y: auto;
  padding: 10px 12px;
  line-height: 1.7;
  outline: none;
}
.rich-content:empty::before {
  content: attr(data-placeholder);
  color: #c0c4cc;
}
.rich-content :deep(ul),
.rich-content :deep(ol) {
  padding-left: 24px;
}
/* 回收站卡片 */
.trash-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 16px;
  background: #fafafa;
  opacity: 0.85;
}
.trash-card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.trash-title {
  font-weight: 600;
  font-size: 15px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.trash-card-content {
  min-height: 40px;
  max-height: 120px;
  overflow: hidden;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 8px;
}
.trash-card-time {
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}
.trash-card-actions {
  display: flex;
  gap: 8px;
}

/* ===== 标签筛选面板 ===== */
.pagination-bar {
  display: flex;
  justify-content: center;
  margin: 24px 0 8px;
}
.tag-filter-panel {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 16px;
  overflow: hidden;
}
.tag-filter-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  cursor: pointer;
  background: #f5f7fa;
  user-select: none;
}
.tag-filter-head:hover {
  background: #ecf5ff;
}
.tag-filter-title {
  font-size: 14px;
  color: #606266;
  flex: 1;
}
.collapse-arrow {
  transition: transform 0.2s;
}
.collapse-arrow.collapsed {
  transform: rotate(-90deg);
}
.tag-filter-body {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}
.tag-filter-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.tag-carousel {
  display: flex;
  align-items: center;
  gap: 4px;
}
.tag-nav-btn {
  flex-shrink: 0;
  padding: 4px 8px;
}
.tag-page {
  flex: 1;
  overflow: hidden;
}
.tag-checkbox-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px 12px;
}
.tag-checkbox-item {
  margin-right: 0;
  display: flex;
  align-items: center;
}
/* 推拉动画：下一页 */
.tag-slide-next-enter-active,
.tag-slide-next-leave-active,
.tag-slide-prev-enter-active,
.tag-slide-prev-leave-active {
  transition: transform 0.3s ease;
}
.tag-slide-next-enter-from { transform: translateX(100%); }
.tag-slide-next-leave-to { transform: translateX(-100%); }
/* 推拉动画：上一页 */
.tag-slide-prev-enter-from { transform: translateX(-100%); }
.tag-slide-prev-leave-to { transform: translateX(100%); }
.tag-count {
  font-size: 12px;
  color: #909399;
  margin-left: 4px;
}

/* ===== 词云 ===== */
.word-cloud {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 12px 16px;
  padding: 24px 12px;
  min-height: 260px;
  background: linear-gradient(135deg, #f5f7fa 0%, #ecf5ff 100%);
  border-radius: 8px;
}
.wc-word {
  cursor: default;
  font-weight: 600;
  transition: transform 0.15s;
  display: inline-block;
}
.wc-word:hover {
  transform: scale(1.15);
}
.wc-tip {
  text-align: center;
  color: #909399;
  font-size: 12px;
  margin-top: 12px;
}
</style>
