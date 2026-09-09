<template>
  <div class="todo-page">
    <el-page-header class="page-header" @back="goBack" content="待办任务"></el-page-header>

    <div class="page-toolbar">
      <div>
        <el-button type="primary" @click="openCreate">新建待办</el-button>
        <el-button :loading="loading" @click="loadTodos">刷新</el-button>
      </div>
      <el-tag v-if="username" type="info" effect="plain">{{ username }} 的待办</el-tag>
    </div>

    <div v-loading="loading">
      <section class="todo-group">
        <div class="group-title">置顶待办（{{ topTodos.length }}）</div>
        <el-empty v-if="topTodos.length === 0" description="暂无置顶待办" :image-size="70"></el-empty>
        <template v-else>
          <el-card v-for="todo in topTodos" :key="todo.id" shadow="hover" class="todo-card">
            <template #header>
              <div class="todo-header">
                <div class="todo-title">
                  <el-tag v-if="todo.top" type="warning" size="small">置顶</el-tag>
                  <el-tag v-if="todo.daily" type="success" size="small">每日</el-tag>
                  <span>{{ todo.title }}</span>
                </div>
                <div class="todo-actions">
                  <el-tag v-if="todo.label" type="primary" effect="plain" size="small">{{ todo.label }}</el-tag>
                  <el-button link type="primary" @click="openEdit(todo)">编辑</el-button>
                  <el-button link type="danger" @click="removeTodo(todo)">删除</el-button>
                </div>
              </div>
            </template>
            <div class="todo-time">
              <span>开始：{{ formatTime(todo.startTime) }}</span>
              <span>结束：{{ formatTime(todo.endTime) }}</span>
              <span v-if="todo.daily && todo.repeatUntil" class="repeat-tip">每日重复至 {{ todo.repeatUntil }}</span>
              <span v-else-if="todo.daily" class="repeat-tip">每天重复，长期有效</span>
            </div>
            <div v-if="todo.content" class="todo-content" v-html="todo.content"></div>
            <div class="todo-meta">
              创建于 {{ formatTime(todo.createdAt) }} · 修改于 {{ formatTime(todo.updatedAt) }}
            </div>
          </el-card>
        </template>
      </section>

      <section class="todo-group">
        <div class="group-title">未置顶待办（{{ normalTodos.length }}）</div>
        <el-empty v-if="normalTodos.length === 0" description="暂无待办，点击右上角新建" :image-size="70"></el-empty>
        <template v-else>
          <el-card v-for="todo in normalTodos" :key="todo.id" shadow="hover" class="todo-card">
            <template #header>
              <div class="todo-header">
                <div class="todo-title">
                  <el-tag v-if="todo.daily" type="success" size="small">每日</el-tag>
                  <span>{{ todo.title }}</span>
                </div>
                <div class="todo-actions">
                  <el-tag v-if="todo.label" type="primary" effect="plain" size="small">{{ todo.label }}</el-tag>
                  <el-button link type="primary" @click="openEdit(todo)">编辑</el-button>
                  <el-button link type="danger" @click="removeTodo(todo)">删除</el-button>
                </div>
              </div>
            </template>
            <div class="todo-time">
              <span>开始：{{ formatTime(todo.startTime) }}</span>
              <span>结束：{{ formatTime(todo.endTime) }}</span>
              <span v-if="todo.daily && todo.repeatUntil" class="repeat-tip">每日重复至 {{ todo.repeatUntil }}</span>
              <span v-else-if="todo.daily" class="repeat-tip">每天重复，长期有效</span>
            </div>
            <div v-if="todo.content" class="todo-content" v-html="todo.content"></div>
            <div class="todo-meta">
              创建于 {{ formatTime(todo.createdAt) }} · 修改于 {{ formatTime(todo.updatedAt) }}
            </div>
          </el-card>
        </template>
      </section>
    </div>

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
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveTodo">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { mapState } from 'vuex'
import RichEditor from '../components/RichEditor.vue'

const DEFAULT_TAGS = ['学习', '生活', '科研', '出行']

function defaultForm() {
  return {
    id: null,
    title: '',
    content: '',
    label: '',
    startTime: '',
    endTime: '',
    daily: false,
    repeatUntil: '',
    top: false
  }
}

export default {
  name: 'TodoPage',
  components: {
    RichEditor
  },
  data() {
    return {
      todos: [],
      loading: false,
      saving: false,
      dialogVisible: false,
      form: defaultForm()
    }
  },
  computed: {
    ...mapState(['username']),
    topTodos() {
      return this.todos.filter(todo => todo.top)
    },
    normalTodos() {
      return this.todos.filter(todo => !todo.top)
    },
    labelOptions() {
      const options = new Set(DEFAULT_TAGS)
      this.todos.forEach(todo => {
        if (todo.label) options.add(todo.label)
      })
      if (this.form.label && !options.has(this.form.label)) {
        options.add(this.form.label)
      }
      return Array.from(options)
    }
  },
  created() {
    this.loadTodos()
  },
  methods: {
    async loadTodos() {
      this.loading = true
      try {
        const res = await this.$http.get('/api/todo/list')
        if (res.data.code === 200) {
          this.todos = res.data.data || []
        } else {
          this.$message.error(res.data.message || '加载待办失败')
        }
      } catch (err) {
        this.handleError(err)
      } finally {
        this.loading = false
      }
    },
    openCreate() {
      this.form = defaultForm()
      this.dialogVisible = true
    },
    openEdit(todo) {
      this.form = {
        id: todo.id,
        title: todo.title || '',
        content: todo.content || '',
        label: todo.label || '',
        startTime: todo.startTime || '',
        endTime: todo.endTime || '',
        daily: !!todo.daily,
        repeatUntil: todo.repeatUntil || '',
        top: !!todo.top
      }
      this.dialogVisible = true
    },
    handleDailyChange(value) {
      if (!value) {
        this.form.repeatUntil = ''
      }
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
      const start = this.parseLocal(this.form.startTime)
      const end = this.parseLocal(this.form.endTime)
      if (end.getTime() <= start.getTime()) {
        this.$message.warning('结束时间必须晚于开始时间')
        return
      }
      if (this.form.daily && this.form.repeatUntil) {
        const repeatEnd = this.parseLocal(this.form.repeatUntil + ' 00:00')
        const startDate = new Date(start.getFullYear(), start.getMonth(), start.getDate())
        if (repeatEnd < startDate) {
          this.$message.warning('每日重复截止日期不能早于开始日期')
          return
        }
      }

      const payload = {
        title: this.form.title,
        content: this.form.content || '',
        label: this.form.label || null,
        startTime: this.form.startTime,
        endTime: this.form.endTime,
        daily: this.form.daily,
        repeatUntil: this.form.daily ? this.form.repeatUntil || null : null,
        top: this.form.top
      }

      this.saving = true
      try {
        const url = this.form.id ? `/api/todo/${this.form.id}` : '/api/todo'
        const request = this.form.id ? this.$http.put(url, payload) : this.$http.post(url, payload)
        const res = await request
        if (res.data.code === 200) {
          this.$message.success(res.data.message || '保存成功')
          this.dialogVisible = false
          this.loadTodos()
        } else {
          this.$message.error(res.data.message || '保存失败')
        }
      } catch (err) {
        this.handleError(err)
      } finally {
        this.saving = false
      }
    },
    removeTodo(todo) {
      this.$confirm(`确定删除待办“${todo.title}”吗？`, '删除确认', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      })
        .then(async () => {
          try {
            const res = await this.$http.delete(`/api/todo/${todo.id}`)
            if (res.data.code === 200) {
              this.$message.success('删除成功')
              this.loadTodos()
            } else {
              this.$message.error(res.data.message || '删除失败')
            }
          } catch (err) {
            this.handleError(err)
          }
        })
        .catch(() => {})
    },
    repeatUntilDisabled(date) {
      if (!this.form.startTime) return false
      const start = this.parseLocal(this.form.startTime)
      const dayStart = new Date(start.getFullYear(), start.getMonth(), start.getDate())
      return date.getTime() < dayStart.getTime()
    },
    parseLocal(value) {
      // "yyyy-MM-dd HH:mm" -> 本地时间 Date
      return new Date(value.replace(' ', 'T'))
    },
    formatTime(value) {
      if (!value) return ''
      return String(value).replace('T', ' ').slice(0, 16)
    },
    resetForm() {
      this.form = defaultForm()
    },
    goBack() {
      this.$router.push('/')
    },
    handleError(err) {
      // 401 已由响应拦截器统一处理
      if (!err.response || err.response.status !== 401) {
        this.$message.error((err.response && err.response.data && err.response.data.message) || err.message)
      }
    }
  }
}
</script>

<style scoped>
.todo-page {
  max-width: 980px;
  margin: 0 auto;
  padding: 18px;
  text-align: left;
}

.page-header {
  margin-bottom: 16px;
}

.page-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
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

.todo-card {
  margin-bottom: 12px;
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
}

.todo-actions {
  display: flex;
  align-items: center;
  gap: 6px;
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

.label-tip {
  width: 100%;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  line-height: 1.4;
}
</style>
