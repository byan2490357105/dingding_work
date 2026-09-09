<template>
  <div class="notes-page">
    <el-menu :default-active="activeIndex" class="el-menu-demo" mode="horizontal">
      <el-menu-item index="1" @click="$router.push('/')">主页</el-menu-item>
      <el-menu-item index="2">
        <span style="font-weight: 600">随手记</span>
      </el-menu-item>
    </el-menu>

    <div class="notes-body">
      <!-- 工具栏：新建笔记按钮 -->
      <div class="notes-toolbar">
        <h2 class="page-title">我的随手记</h2>
        <el-button type="primary" size="large" @click="openCreate">
          <el-icon><EditPen /></el-icon>&nbsp;新建笔记
        </el-button>
      </div>

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
        <el-empty v-if="normalNotes.length === 0" description="还没有笔记，点击上方“新建笔记”开始记录吧" :image-size="60" />
        <el-row :gutter="16">
          <el-col v-for="note in normalNotes" :key="note.id" :span="8">
            <note-card
              :note="note"
              @edit="openEdit"
              @toggle-pin="togglePin"
              @remove="removeNote"
            />
          </el-col>
        </el-row>
      </div>
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
      activeIndex: '2',
      // 默认标签
      defaultTags: ['学习', '生活', '科研', '出行'],
      pinnedNotes: [],
      normalNotes: [],
      dialogVisible: false,
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
  },
  methods: {
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
      this.$confirm('确定删除这条笔记吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await this.$http.delete(`/api/notes/${note.id}`)
          if (res.data.code === 200) {
            this.$message.success('删除成功')
            this.fetchNotes()
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
</style>
