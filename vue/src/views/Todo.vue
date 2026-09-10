<template>
  <div class="todo-page">
    <h2 class="page-heading">待办任务</h2>

    <div class="page-toolbar">
      <div>
        <el-button type="primary" @click="openCreate">新建待办</el-button>
        <el-button :loading="loading" @click="loadTodos">刷新</el-button>
      </div>
      <el-tag v-if="username" type="info" effect="plain">{{ username }} 的待办</el-tag>
    </div>

    <el-tabs v-model="activeTab" class="todo-tabs" @tab-change="onTabChange">
      <el-tab-pane label="我的待办" name="todos">
        <div v-loading="loading">
          <!-- 置顶待办 -->
          <section class="todo-group">
            <div class="group-title">置顶待办（{{ topTodos.length }}）</div>
            <el-empty v-if="topTodos.length === 0" description="暂无置顶待办" :image-size="70"></el-empty>
            <template v-else>
              <el-card
                v-for="todo in topTodos"
                :key="todo.id"
                shadow="hover"
                class="todo-card"
                :class="{ 'todo-overdue': todo.status === '逾期' }"
              >
                <template #header>
                  <div class="todo-header">
                    <div class="todo-title">
                      <el-checkbox
                        :model-value="todo.completed"
                        title="标记完成 / 取消完成"
                        @change="toggleComplete(todo)"
                      />
                      <el-tag v-if="todo.top" type="warning" size="small">置顶</el-tag>
                      <el-tag v-if="todo.daily" type="success" size="small">每日</el-tag>
                      <el-tag :type="statusType(todo.status)" size="small" effect="dark">{{ todo.status }}</el-tag>
                      <span class="todo-title-text">{{ todo.title }}</span>
                    </div>
                    <div class="todo-actions">
                      <el-tag v-if="todo.label" type="primary" effect="plain" size="small">{{ todo.label }}</el-tag>
                      <el-button link type="primary" @click="openDetail(todo)">详情</el-button>
                      <el-button link type="success" @click="toggleComplete(todo)">标记完成</el-button>
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

          <!-- 未置顶待办 -->
          <section class="todo-group">
            <div class="group-title">未置顶待办（{{ normalTodos.length }}）</div>
            <el-empty v-if="normalTodos.length === 0" description="暂无待办，点击右上角新建" :image-size="70"></el-empty>
            <template v-else>
              <el-card
                v-for="todo in normalTodos"
                :key="todo.id"
                shadow="hover"
                class="todo-card"
                :class="{ 'todo-overdue': todo.status === '逾期' }"
              >
                <template #header>
                  <div class="todo-header">
                    <div class="todo-title">
                      <el-checkbox
                        :model-value="todo.completed"
                        title="标记完成 / 取消完成"
                        @change="toggleComplete(todo)"
                      />
                      <el-tag v-if="todo.daily" type="success" size="small">每日</el-tag>
                      <el-tag :type="statusType(todo.status)" size="small" effect="dark">{{ todo.status }}</el-tag>
                      <span class="todo-title-text">{{ todo.title }}</span>
                    </div>
                    <div class="todo-actions">
                      <el-tag v-if="todo.label" type="primary" effect="plain" size="small">{{ todo.label }}</el-tag>
                      <el-button link type="primary" @click="openDetail(todo)">详情</el-button>
                      <el-button link type="success" @click="toggleComplete(todo)">标记完成</el-button>
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

          <!-- 已完成待办 -->
          <section class="todo-group">
            <div class="group-title">已完成（{{ completedTodos.length }}）</div>
            <el-empty v-if="completedTodos.length === 0" description="暂无已完成的待办" :image-size="70"></el-empty>
            <template v-else>
              <el-card
                v-for="todo in completedTodos"
                :key="todo.id"
                shadow="hover"
                class="todo-card todo-done"
              >
                <template #header>
                  <div class="todo-header">
                    <div class="todo-title">
                      <el-checkbox
                        :model-value="todo.completed"
                        title="取消完成"
                        @change="toggleComplete(todo)"
                      />
                      <el-tag v-if="todo.top" type="warning" size="small">置顶</el-tag>
                      <el-tag type="success" size="small" effect="dark">已完成</el-tag>
                      <span class="todo-title-text done-title">{{ todo.title }}</span>
                    </div>
                    <div class="todo-actions">
                      <el-tag v-if="todo.label" type="primary" effect="plain" size="small">{{ todo.label }}</el-tag>
                      <el-button link type="warning" @click="toggleComplete(todo)">取消完成</el-button>
                      <el-button link type="primary" @click="openEdit(todo)">编辑</el-button>
                      <el-button link type="danger" @click="removeTodo(todo)">删除</el-button>
                    </div>
                  </div>
                </template>
                <div class="todo-time">
                  <span>开始：{{ formatTime(todo.startTime) }}</span>
                  <span>结束：{{ formatTime(todo.endTime) }}</span>
                </div>
                <div class="todo-meta">
                  创建于 {{ formatTime(todo.createdAt) }} · 修改于 {{ formatTime(todo.updatedAt) }}
                </div>
              </el-card>
            </template>
          </section>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="`回收站(${trashTodos.length})`" name="trash">
        <el-empty v-if="trashTodos.length === 0" description="回收站是空的" :image-size="80"></el-empty>
        <template v-else>
          <el-card
            v-for="todo in trashTodos"
            :key="todo.id"
            shadow="never"
            class="todo-card trash-todo-card"
          >
            <template #header>
              <div class="todo-header">
                <div class="todo-title">
                  <el-tag v-if="todo.top" type="warning" size="small">置顶</el-tag>
                  <span class="todo-title-text">{{ todo.title }}</span>
                </div>
                <div class="todo-actions">
                  <el-tag v-if="todo.label" type="info" effect="plain" size="small">{{ todo.label }}</el-tag>
                  <el-button link type="success" @click="restoreTodo(todo)">恢复</el-button>
                  <el-button link type="danger" @click="permanentlyDeleteTodo(todo)">彻底删除</el-button>
                </div>
              </div>
            </template>
            <div class="todo-time">
              <span>开始：{{ formatTime(todo.startTime) }}</span>
              <span>结束：{{ formatTime(todo.endTime) }}</span>
            </div>
            <div class="todo-meta">
              创建于 {{ formatTime(todo.createdAt) }} · 修改于 {{ formatTime(todo.updatedAt) }}
            </div>
          </el-card>
        </template>
      </el-tab-pane>
    </el-tabs>

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

    <!-- 待办详情弹窗 -->
    <el-dialog v-model="detailVisible" title="待办详情" width="680px">
      <div v-if="detail" class="todo-detail">
        <h2 class="detail-title">
          <el-tag :type="statusType(detail.status)" size="small" effect="dark">{{ detail.status }}</el-tag>
          <span :class="{ 'done-title': detail.completed }">{{ detail.title }}</span>
        </h2>

        <el-descriptions :column="1" border size="small" class="detail-desc">
          <el-descriptions-item label="标签">{{ detail.label || '无' }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ formatTime(detail.startTime) }}</el-descriptions-item>
          <el-descriptions-item label="截止时间">
            <span :class="{ 'overdue-text': detail.status === '逾期' }">{{ formatTime(detail.endTime) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="重复">
            <template v-if="detail.daily">
              每日重复<template v-if="detail.repeatUntil">，截止 {{ detail.repeatUntil }}</template>
            </template>
            <template v-else>不重复</template>
          </el-descriptions-item>
          <el-descriptions-item label="置顶">{{ detail.top ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(detail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="修改时间">{{ formatTime(detail.updatedAt) }}</el-descriptions-item>
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
      trashTodos: [],
      activeTab: 'todos',
      loading: false,
      saving: false,
      dialogVisible: false,
      detailVisible: false,
      detail: null,
      form: defaultForm()
    }
  },
  computed: {
    ...mapState(['username']),
    // 置顶且未完成的待办
    topTodos() {
      return this.todos.filter(todo => todo.top && !todo.completed)
    },
    // 未置顶且未完成的待办
    normalTodos() {
      return this.todos.filter(todo => !todo.top && !todo.completed)
    },
    // 已完成的待办（不论置顶）
    completedTodos() {
      return this.todos.filter(todo => todo.completed)
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
    this.fetchTrash()
  },
  methods: {
    // 切换 Tab 时刷新回收站
    onTabChange(tab) {
      if (tab === 'trash') {
        this.fetchTrash()
      }
    },
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
    // 状态 -> 标签颜色：已完成绿色、逾期红色、进行中蓝色
    statusType(status) {
      if (status === '已完成') return 'success'
      if (status === '逾期') return 'danger'
      return 'primary'
    },
    // 标记完成 / 取消完成
    async toggleComplete(todo) {
      const next = !todo.completed
      try {
        const res = await this.$http.put(`/api/todo/${todo.id}/complete`, null, {
          params: { completed: next }
        })
        if (res.data.code === 200) {
          this.$message.success(next ? '已标记为完成' : '已取消完成')
          // 详情弹窗打开时同步刷新详情数据
          if (this.detailVisible && this.detail && this.detail.id === todo.id && res.data.data) {
            this.detail = res.data.data
          }
          this.loadTodos()
        } else {
          this.$message.error(res.data.message || '操作失败')
        }
      } catch (err) {
        this.handleError(err)
      }
    },
    // 打开详情（请求最新数据）
    async openDetail(todo) {
      try {
        const res = await this.$http.get(`/api/todo/${todo.id}`)
        if (res.data.code === 200) {
          this.detail = res.data.data
          this.detailVisible = true
        } else {
          this.$message.error(res.data.message || '加载详情失败')
        }
      } catch (err) {
        this.handleError(err)
      }
    },
    toggleCompleteFromDetail() {
      if (this.detail) {
        this.toggleComplete(this.detail)
      }
    },
    editFromDetail() {
      const todo = this.detail
      this.detailVisible = false
      this.openEdit(todo)
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
      this.$confirm(`确定删除待办"${todo.title}"吗？删除后可在回收站找回。`, '删除确认', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      })
        .then(async () => {
          try {
            const res = await this.$http.delete(`/api/todo/${todo.id}`)
            if (res.data.code === 200) {
              this.$message.success('已移入回收站')
              this.loadTodos()
              this.fetchTrash()
            } else {
              this.$message.error(res.data.message || '删除失败')
            }
          } catch (err) {
            this.handleError(err)
          }
        })
        .catch(() => {})
    },
    // 拉取回收站待办列表
    async fetchTrash() {
      try {
        const res = await this.$http.get('/api/todo/trash')
        if (res.data.code === 200) {
          this.trashTodos = res.data.data || []
        }
      } catch (err) {
        // 忽略 401（拦截器统一处理）
      }
    },
    // 从回收站恢复待办
    async restoreTodo(todo) {
      try {
        const res = await this.$http.put(`/api/todo/restore/${todo.id}`)
        if (res.data.code === 200) {
          this.$message.success('恢复成功')
          this.fetchTrash()
          this.loadTodos()
        } else {
          this.$message.error(res.data.message || '恢复失败')
        }
      } catch (err) {
        this.handleError(err)
      }
    },
    // 彻底删除待办（不可恢复）
    permanentlyDeleteTodo(todo) {
      this.$confirm(`确定彻底删除待办"${todo.title}"吗？此操作不可恢复！`, '危险操作', {
        type: 'error',
        confirmButtonText: '彻底删除',
        cancelButtonText: '取消'
      })
        .then(async () => {
          try {
            const res = await this.$http.delete(`/api/todo/permanent/${todo.id}`)
            if (res.data.code === 200) {
              this.$message.success('已彻底删除')
              this.fetchTrash()
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

.page-heading {
  margin: 0 0 16px;
  font-size: 22px;
  font-weight: 700;
  color: #303133;
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

/* 逾期任务：卡片左侧红色提醒条 */
.todo-overdue {
  border-left: 4px solid var(--el-color-danger);
}

/* 已完成任务：整体淡化 */
.todo-done {
  opacity: 0.75;
}

/* 已完成标题：删除线 */
.done-title {
  text-decoration: line-through;
  color: var(--el-text-color-placeholder);
}

.todo-title-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.overdue-text {
  color: var(--el-color-danger);
  font-weight: 600;
}

/* 待办详情弹窗 */
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
.trash-todo-card {
  opacity: 0.85;
  border-style: dashed;
}
</style>
