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
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button size="large" @click="wordCloudVisible = true">
            <el-icon><Histogram /></el-icon>&nbsp;词云
          </el-button>
          <el-button size="large" :loading="exportingCsv" @click="exportCsv">
            <el-icon><Download /></el-icon>&nbsp;导出CSV
          </el-button>
          <el-button size="large" type="success" plain :loading="exportingWord" @click="exportWord">
            <el-icon><Document /></el-icon>&nbsp;导出Word
          </el-button>
          <el-button size="large" type="warning" @click="$refs.reportPanel.open('monthly')">
            <el-icon><MagicStick /></el-icon>&nbsp;AI 月报
          </el-button>
          <el-button size="large" type="warning" plain @click="$refs.reportPanel.open('weekly')">
            <el-icon><MagicStick /></el-icon>&nbsp;AI 周报
          </el-button>
          <el-button type="primary" size="large" @click="openCreate">
            <el-icon><EditPen /></el-icon>&nbsp;新建笔记
          </el-button>
        </div>
      </div>

      <!-- 标签筛选面板（可折叠） -->
      <tag-filter-panel
        v-model="selectedTags"
        v-model:collapsed="tagPanelCollapsed"
        :stats="tagStats"
        hint="按标签筛选（勾选显示，取消勾选则隐藏该标签的笔记）"
      />

      <el-tabs v-model="activeTab" class="notes-tabs" @tab-change="onTabChange">
        <el-tab-pane label="我的笔记" name="notes">
          <!-- 置顶笔记（置顶排序在最前，翻到后续页无置顶时整块隐藏） -->
          <div class="section" v-if="pinnedNotes.length > 0">
            <h3 class="section-title">
              <el-icon><Top /></el-icon> 置顶笔记
              <el-tag size="small" type="warning" round>{{ pinnedNotes.length }}</el-tag>
            </h3>
            <el-row :gutter="16">
              <el-col v-for="note in pinnedNotes" :key="note.id" :span="8">
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
              <el-tag size="small" type="info" round>{{ normalNotes.length }}</el-tag>
            </h3>
            <el-empty
              v-if="normalNotes.length === 0"
              :description="emptyDescription"
              :image-size="60"
            />
            <el-row :gutter="16">
              <el-col v-for="note in normalNotes" :key="note.id" :span="8">
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
                <div class="trash-card-time">修改于 {{ formatMinute(note.updateTime) }}</div>
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
          <span>创建：{{ formatMinute(detail.createTime) }}</span>
          <span>修改：{{ formatMinute(detail.updateTime) }}</span>
        </div>
        <el-divider />
        <div class="detail-content" v-html="detail.content || emptyContent"></div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="editFromDetail">编辑</el-button>
      </template>
    </el-dialog>

    <!-- AI 笔记月报 / 周报 + 历史报告 -->
    <ai-report-panel ref="reportPanel" kind="note" />

    <!-- 词云弹窗 -->
    <word-cloud-dialog v-model="wordCloudVisible" kind="note" />
  </div>
</template>

<script>
import { Document, EditPen, Download, MagicStick, Search, Top, Histogram } from '@element-plus/icons-vue'
import NoteCard from '../components/NoteCard.vue'
import TagFilterPanel from '../components/TagFilterPanel.vue'
import WordCloudDialog from '../components/WordCloudDialog.vue'
import AiReportPanel from '../components/AiReportPanel.vue'
import { notesApi } from '../api/notes'
import { formatMinute } from '../utils/date'
import { debounce } from '../composables/useSearchDebounce'
import { downloadBlob } from '../utils/download'
import { notifyError } from '../utils/notify'

/** 默认标签 */
const DEFAULT_TAGS = ['学习', '生活', '科研', '出行']

/** 拉取标签统计并归一化为 [{ name, count }] */
const loadTagStats = async () =>
  ((await notesApi.tagStats()) || []).map(item => ({ name: item.tag, count: item.count }))

export default {
  name: 'Notes',
  components: {
    Document,
    EditPen,
    Download,
    MagicStick,
    Search,
    Top,
    Histogram,
    NoteCard,
    TagFilterPanel,
    WordCloudDialog,
    AiReportPanel
  },
  data() {
    return {
      defaultTags: DEFAULT_TAGS,
      activeTab: 'notes',
      // 分页数据（后端分页）
      pageRecords: [],
      pageNum: 1,
      pageSize: 9,
      pageTotal: 0,
      // 标签统计（供筛选面板与下拉选项）
      tagStats: [],
      selectedTags: [],
      tagPanelCollapsed: false,
      trashNotes: [],
      exportingCsv: false,
      exportingWord: false,
      dialogVisible: false,
      detailVisible: false,
      detail: null,
      saving: false,
      searchKeyword: '',
      wordCloudVisible: false,
      form: { id: null, title: '', content: '', tag: '' }
    }
  },
  computed: {
    emptyContent() {
      return '<span style="color:#909399">（无内容）</span>'
    },
    emptyDescription() {
      const filtered = this.searchKeyword || this.selectedTags.length < this.tagStats.length
      return filtered ? '没有符合条件的笔记' : '还没有笔记，点击上方新建笔记开始记录吧'
    },
    tagSelectOptions() {
      const set = new Set(this.defaultTags)
      this.tagStats.forEach(item => set.add(item.name))
      if (this.form.tag && !set.has(this.form.tag)) set.add(this.form.tag)
      return Array.from(set)
    },
    // 当前页内的置顶笔记
    pinnedNotes() {
      return this.pageRecords.filter(n => n.isPinned === 1)
    },
    // 当前页内的未置顶笔记
    normalNotes() {
      return this.pageRecords.filter(n => n.isPinned !== 1)
    }
  },
  watch: {
    // 搜索关键字防抖 300ms 后重新查询并回到第 1 页
    searchKeyword() {
      this.reloadDebounced()
    },
    // 勾选标签变化立即重新查询
    selectedTags() {
      this.pageNum = 1
      this.fetchPage()
    }
  },
  created() {
    // 每个实例各持一个防抖函数，避免多实例共用定时器
    this.reloadDebounced = debounce(() => {
      this.pageNum = 1
      this.fetchPage()
    }, 300)
    this.fetchPage()
    this.fetchTagStats()
    this.fetchTrash()
  },
  beforeUnmount() {
    this.reloadDebounced.cancel()
  },
  methods: {
    formatMinute,
    // 拉取分页笔记（关键字 / 未勾选标签下沉后端排除）
    async fetchPage() {
      try {
        const params = { pageNum: this.pageNum, pageSize: this.pageSize }
        if (this.searchKeyword) params.keyword = this.searchKeyword
        const excluded = this.tagStats
          .map(item => item.name)
          .filter(name => !this.selectedTags.includes(name))
        if (excluded.length) params.excludeTag = excluded
        const data = await notesApi.page(params)
        this.pageRecords = data.records || []
        this.pageTotal = data.total || 0
      } catch (err) {
        notifyError(err, '加载笔记失败')
      }
    },
    async fetchTagStats() {
      try {
        this.tagStats = await loadTagStats()
      } catch (err) {
        // 标签统计失败不阻断主流程
      }
    },
    onPageChange(page) {
      this.pageNum = page
      this.fetchPage()
    },
    onSizeChange(size) {
      this.pageSize = size
      this.pageNum = 1
      this.fetchPage()
    },
    // 切换 Tab 时刷新回收站
    onTabChange(tab) {
      if (tab === 'trash') {
        this.fetchTrash()
      }
    },

    // ===== 导出 =====
    async exportCsv() {
      this.exportingCsv = true
      try {
        await downloadBlob(notesApi.exportFile('csv'), '笔记导出.csv')
        this.$message.success('笔记已导出为 CSV 文件')
      } catch (err) {
        notifyError(err, '导出失败，请稍后重试')
      } finally {
        this.exportingCsv = false
      }
    },
    async exportWord() {
      this.exportingWord = true
      try {
        await downloadBlob(notesApi.exportFile('word'), '随手记.docx')
        this.$message.success('笔记已导出为 Word 文档')
      } catch (err) {
        notifyError(err, '导出失败，请稍后重试')
      } finally {
        this.exportingWord = false
      }
    },

    // ===== 详情 / 编辑 =====
    async openView(note) {
      try {
        this.detail = await notesApi.detail(note.id)
        this.detailVisible = true
      } catch (err) {
        notifyError(err, '加载详情失败')
      }
    },
    editFromDetail() {
      const note = this.detail
      this.detailVisible = false
      this.openEdit(note)
    },
    openCreate() {
      this.form = { id: null, title: '', content: '', tag: '' }
      this.dialogVisible = true
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
      this.$nextTick(() => {
        if (this.$refs.editorRef) this.$refs.editorRef.innerHTML = note.content || ''
      })
    },
    // 富文本命令（@mousedown.prevent 已阻止按钮抢夺光标）
    exec(command) {
      document.execCommand(command, false, null)
      if (this.$refs.editorRef) this.$refs.editorRef.focus()
    },
    onDialogClosed() {
      if (this.$refs.editorRef) this.$refs.editorRef.innerHTML = ''
    },
    async saveNote() {
      if (!this.form.title.trim()) {
        this.$message.warning('请填写笔记标题')
        return
      }
      const payload = {
        title: this.form.title.trim(),
        content: this.$refs.editorRef ? this.$refs.editorRef.innerHTML : '',
        tag: this.form.tag || ''
      }
      if (this.form.id) payload.id = this.form.id

      this.saving = true
      try {
        await notesApi.save(payload)
        this.$message.success(this.form.id ? '修改成功' : '新建成功')
        this.dialogVisible = false
        this.fetchPage()
        this.fetchTagStats()
      } catch (err) {
        notifyError(err, '保存失败')
      } finally {
        this.saving = false
      }
    },
    async togglePin(note) {
      try {
        await notesApi.togglePin(note.id)
        this.$message.success(note.isPinned === 1 ? '已取消置顶' : '已置顶')
        this.fetchPage()
        this.fetchTagStats()
      } catch (err) {
        notifyError(err, '操作失败')
      }
    },
    removeNote(note) {
      this.$confirm('确定删除这条笔记吗？删除后可在回收站找回。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(async () => {
          try {
            await notesApi.remove(note.id)
            this.$message.success('已移入回收站')
            this.fetchPage()
            this.fetchTagStats()
            this.fetchTrash()
          } catch (err) {
            notifyError(err, '删除失败')
          }
        })
        .catch(() => {})
    },

    // ===== 回收站 =====
    async fetchTrash() {
      try {
        this.trashNotes = (await notesApi.trash()) || []
      } catch (err) {
        // 忽略 401（拦截器统一处理）
      }
    },
    async restoreNote(note) {
      try {
        await notesApi.restore(note.id)
        this.$message.success('恢复成功')
        this.fetchTrash()
        this.fetchPage()
        this.fetchTagStats()
      } catch (err) {
        notifyError(err, '恢复失败')
      }
    },
    permanentlyDeleteNote(note) {
      this.$confirm(`确定彻底删除笔记"${note.title}"吗？此操作不可恢复！`, '危险操作', {
        confirmButtonText: '彻底删除',
        cancelButtonText: '取消',
        type: 'error'
      })
        .then(async () => {
          try {
            await notesApi.permanentRemove(note.id)
            this.$message.success('已彻底删除')
            this.fetchTrash()
          } catch (err) {
            notifyError(err, '删除失败')
          }
        })
        .catch(() => {})
    }
  }
}
</script>

<style scoped>
.notes-body {
  padding: 24px 20px 40px;
  max-width: 1100px;
  margin: 0 auto;
}
.notes-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
  padding: 4px 0;
}
.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--wb-text-primary);
  display: flex;
  align-items: center;
  gap: 10px;
}
.page-title::before {
  content: '';
  width: 5px;
  height: 22px;
  border-radius: 3px;
  background: var(--wb-gradient-brand);
  display: inline-block;
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
.notes-page {
  text-align: left;
}
.pagination-bar {
  display: flex;
  justify-content: center;
  margin: 24px 0 8px;
}
/* 富文本编辑区 */
.rich-editor {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
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
/* 详情 */
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
</style>
