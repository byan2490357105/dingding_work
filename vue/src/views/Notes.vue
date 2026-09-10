<template>
  <div class="notes-page">
    <div class="notes-body">
      <!-- 工具栏：新建笔记按钮 -->
      <div class="notes-toolbar">
        <h2 class="page-title">我的随手记</h2>
        <el-button type="primary" size="large" @click="openCreate">
          <el-icon><EditPen /></el-icon>&nbsp;新建笔记
        </el-button>
      </div>

      <el-tabs v-model="activeTab" class="notes-tabs" @tab-change="onTabChange">
        <el-tab-pane label="我的笔记" name="notes">
          <!-- 置顶笔记 -->
          <div class="section">
            <h3 class="section-title">
              <el-icon><Top /></el-icon> 置顶笔记
              <el-tag size="small" type="warning" round>{{ pinnedNotes.length }}</el-tag>
            </h3>
            <el-empty v-if="pinnedNotes.length === 0" description="暂无置顶笔记" :image-size="60" />
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
            <el-empty v-if="normalNotes.length === 0" description="还没有笔记，点击上方新建笔记开始记录吧" :image-size="60" />
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
            <el-option v-for="t in defaultTags" :key="t" :label="t" :value="t" />
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
  </div>
</template>

<script>
import { Top, Document, EditPen } from '@element-plus/icons-vue'
import NoteCard from '../components/NoteCard.vue'

export default {
  name: 'Notes',
  components: { Top, Document, EditPen, NoteCard },
  data() {
    return {
      // 默认标签
      defaultTags: ['学习', '生活', '科研', '出行'],
      activeTab: 'notes',
      pinnedNotes: [],
      normalNotes: [],
      trashNotes: [],
      dialogVisible: false,
      detailVisible: false,
      detail: null,
      saving: false,
      form: {
        id: null,
        title: '',
        content: '',
        tag: ''
      }
    }
  },
  created() {
    this.fetchNotes()
    this.fetchTrash()
  },
  methods: {
    // 切换 Tab 时刷新回收站
    onTabChange(tab) {
      if (tab === 'trash') {
        this.fetchTrash()
      }
    },
    // 拉取当前用户笔记（后端按置顶/未置顶分为两个列表）
    async fetchNotes() {
      try {
        const res = await this.$http.get('/api/notes')
        if (res.data.code === 200) {
          this.pinnedNotes = res.data.data.pinned || []
          this.normalNotes = res.data.data.normal || []
        }
      } catch (err) {
        if (!err.response || err.response.status !== 401) {
          this.$message.error('加载笔记失败')
        }
      }
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
          this.fetchNotes()
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
          this.fetchNotes()
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
            this.fetchNotes()
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
          this.fetchNotes()
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
</style>
