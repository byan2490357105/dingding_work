<template>
  <div class="todo-page">
    <h2 class="page-heading">待办任务</h2>

    <div class="page-toolbar">
      <div>
        <el-button type="primary" @click="openCreate">新建待办</el-button>
        <el-button :type="viewMode === 'calendar' ? 'primary' : 'default'" plain @click="toggleCalendarView">
          <el-icon><Calendar /></el-icon>&nbsp;{{ viewMode === 'calendar' ? '切换列表模式' : '切换日历模式' }}
        </el-button>
        <el-button :type="viewMode === 'timeline' ? 'primary' : 'default'" plain @click="toggleTimelineView">
          <el-icon><AlarmClock /></el-icon>&nbsp;{{ viewMode === 'timeline' ? '切换列表模式' : '时间轴模式' }}
        </el-button>
        <el-button :loading="loading" @click="loadTodos">刷新</el-button>
        <el-button type="success" plain :loading="exporting" @click="exportCsv">
          <el-icon><Download /></el-icon>&nbsp;导出待办
        </el-button>
        <el-button type="warning" :loading="generatingReport" @click="generateTodoReport">
          <el-icon><MagicStick /></el-icon>&nbsp;AI 月报
        </el-button>
        <el-button type="warning" plain :loading="generatingReport" @click="generateTodoWeeklyReport">
          <el-icon><MagicStick /></el-icon>&nbsp;AI 周报
        </el-button>
      </div>
      <el-tag v-if="username" type="info" effect="plain">{{ username }} 的待办</el-tag>
    </div>

    <el-tabs v-if="viewMode === 'list'" v-model="activeTab" class="todo-tabs" @tab-change="onTabChange">
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
                  <span v-if="todo.remindTime" class="remind-tip">
                    <el-icon><AlarmClock /></el-icon>提醒：{{ formatTime(todo.remindTime) }}
                    <el-tag v-if="todo.reminded" size="small" type="success" effect="plain">已提醒</el-tag>
                  </span>
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
                  <span v-if="todo.remindTime" class="remind-tip">
                    <el-icon><AlarmClock /></el-icon>提醒：{{ formatTime(todo.remindTime) }}
                    <el-tag v-if="todo.reminded" size="small" type="success" effect="plain">已提醒</el-tag>
                  </span>
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

    <!-- 日历模式：以开始时间的日期为维度聚合展示 -->
    <div v-else-if="viewMode === 'calendar'" v-loading="loading" class="calendar-view">
      <div class="calendar-card">
        <div class="calendar-toolbar">
          <el-button circle size="large" title="上一月" @click="prevMonth">
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          <div class="calendar-center">
            <div class="calendar-title">{{ calendarYear }} 年 {{ calendarMonth + 1 }} 月</div>
            <el-button link type="primary" @click="goToday">回到本月</el-button>
          </div>
          <el-button circle size="large" title="下一月" @click="nextMonth">
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>

        <div class="calendar-grid">
          <div v-for="w in weekLabels" :key="w" class="calendar-week">{{ w }}</div>
          <div
            v-for="cell in calendarCells"
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
                :title="todo.title + '（' + formatTime(todo.startTime) + '）'"
                @click="openDetail(todo)"
              >
                <span class="chip-time">{{ String(todo.startTime).slice(11, 16) }}</span>
                <span class="chip-title">{{ todo.title }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="calendar-legend">
          <span><span class="legend-dot" style="background: var(--el-color-primary);"></span>进行中</span>
          <span><span class="legend-dot" style="background: var(--el-color-success);"></span>已完成</span>
          <span><span class="legend-dot" style="background: var(--el-color-danger);"></span>逾期</span>
          <span class="legend-tip">任务归属日期以开始时间为准 · 同一天可挂载多条任务 · 点击任务查看详情</span>
        </div>
      </div>
    </div>

    <!-- 时间轴模式：未完成待办的时间线总览 + 时间节点 -->
    <div v-else v-loading="loading" class="timeline-view">
      <el-empty v-if="timelineTodos.length === 0" description="暂无未完成的待办任务" :image-size="90"></el-empty>
      <template v-else>
        <!-- 甘特总览：不同事务使用不同颜色 -->
        <div class="gantt-card">
          <div class="view-title">
            时间线总览
            <span class="view-sub">共 {{ timelineTodos.length }} 个未完成事务 · 不同事务使用不同颜色</span>
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
                  v-if="ganttTodayLeft !== null"
                  class="gantt-today-badge"
                  :style="{ left: ganttTodayLeft + '%' }"
                >今天</span>
              </div>
              <div v-for="item in timelineTodos" :key="'bar-' + item.id" class="gantt-track">
                <div
                  class="gantt-bar"
                  :style="{ left: item.left + '%', width: item.width + '%', background: item.color }"
                  :title="item.title + '：' + formatTime(item.startTime) + ' ~ ' + formatTime(item.endTime)"
                >
                  <span class="gantt-bar-text">
                    {{ formatTime(item.startTime).slice(5, 16) }} ~ {{ formatTime(item.endTime).slice(5, 16) }}
                  </span>
                </div>
              </div>
              <div
                v-if="ganttTodayLeft !== null"
                class="gantt-today-line"
                :style="{ left: ganttTodayLeft + '%' }"
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
              <div class="milestone-item" @click="openDetail(ev.todo)">
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
            placeholder="留空不提醒，到达该时间将发送邮件提醒"
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
          <el-descriptions-item label="开始时间">{{ formatTime(detail.startTime) }}</el-descriptions-item>
          <el-descriptions-item label="截止时间">
            <span :class="{ 'overdue-text': detail.status === '逾期' }">{{ formatTime(detail.endTime) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="提醒时间">
            <span v-if="detail.remindTime">{{ formatTime(detail.remindTime) }}
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

    <!-- AI 待办月报 / 周报弹窗 -->
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
          <el-radio-button label="todo-monthly">月报</el-radio-button>
          <el-radio-button label="todo-weekly">周报</el-radio-button>
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
  </div>
</template>

<script>
import { mapState } from 'vuex'
import { Download, MagicStick, Calendar, AlarmClock, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import RichEditor from '../components/RichEditor.vue'
import { renderMarkdown } from '../utils/markdown'

const DEFAULT_TAGS = ['学习', '生活', '科研', '出行']
// 时间轴模式：不同事务的绘制颜色（循环取用）
const TIMELINE_COLORS = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#9254DE', '#13C2C2', '#EB2F96', '#FAAD14', '#2F54EB', '#7CB305']

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

export default {
  name: 'TodoPage',
  components: {
    Download,
    MagicStick,
    Calendar,
    AlarmClock,
    ArrowLeft,
    ArrowRight,
    RichEditor
  },
  data() {
    return {
      todos: [],
      trashTodos: [],
      activeTab: 'todos',
      // 视图模式：list 列表 / calendar 日历 / timeline 时间轴
      viewMode: 'list',
      calendarYear: new Date().getFullYear(),
      calendarMonth: new Date().getMonth(),
      weekLabels: ['一', '二', '三', '四', '五', '六', '日'],
      loading: false,
      saving: false,
      exporting: false,
      generatingReport: false,
      reportVisible: false,
      reportTitle: 'AI 待办月报',
      reportPeriod: '',
      reportHtml: '',
      reportCached: false,
      historyVisible: false,
      historyLoading: false,
      historyList: [],
      historyType: 'todo-monthly',
      dialogVisible: false,
      detailVisible: false,
      detail: null,
      form: defaultForm(),
      predicting: false,
      prediction: null
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
    },

    // ===== 日历模式 =====
    // 以开始时间的日期（yyyy-MM-dd）为 key 聚合待办：一个日期可挂载多条任务
    todosByDate() {
      const map = {}
      this.todos.forEach(todo => {
        if (!todo.startTime) return
        const key = String(todo.startTime).slice(0, 10)
        if (!map[key]) map[key] = []
        map[key].push(todo)
      })
      Object.keys(map).forEach(key => {
        map[key].sort((a, b) => String(a.startTime).localeCompare(String(b.startTime)))
      })
      return map
    },
    // 当前月份的 6×7 日历网格（周一起始），跨月日期置灰
    calendarCells() {
      const year = this.calendarYear
      const month = this.calendarMonth
      const first = new Date(year, month, 1)
      let offset = first.getDay() - 1 // 周一为第一列：周日(offset=-1)归到末列
      if (offset < 0) offset = 6
      const start = new Date(year, month, 1 - offset)
      const todayKey = this.formatDateKey(new Date())
      const cells = []
      for (let i = 0; i < 42; i++) {
        const d = new Date(start.getFullYear(), start.getMonth(), start.getDate() + i)
        const key = this.formatDateKey(d)
        cells.push({
          key,
          day: d.getDate(),
          inMonth: d.getMonth() === month,
          isToday: key === todayKey,
          todos: this.todosByDate[key] || []
        })
      }
      return cells
    },

    // ===== 时间轴模式 =====
    // 未完成待办的时间范围（甘特横轴刻度依据）
    ganttRange() {
      let min = Infinity
      let max = -Infinity
      this.todos.forEach(todo => {
        if (todo.completed || !todo.startTime || !todo.endTime) return
        const s = this.parseLocal(todo.startTime).getTime()
        const e = this.parseLocal(todo.endTime).getTime()
        if (isNaN(s) || isNaN(e)) return
        if (s < min) min = s
        if (e > max) max = e
      })
      if (min === Infinity || max === -Infinity) return null
      const startDate = new Date(min)
      const endDate = new Date(max)
      const start = new Date(startDate.getFullYear(), startDate.getMonth(), startDate.getDate()).getTime()
      const end = new Date(endDate.getFullYear(), endDate.getMonth(), endDate.getDate() + 1).getTime()
      return { start, end, span: Math.max(1, end - start) }
    },
    // 未完成待办：按开始时间排序，分配颜色并计算甘特条位置
    timelineTodos() {
      const items = []
      this.todos.forEach(todo => {
        if (todo.completed || !todo.startTime || !todo.endTime) return
        const s = this.parseLocal(todo.startTime).getTime()
        const e = this.parseLocal(todo.endTime).getTime()
        if (isNaN(s) || isNaN(e)) return
        items.push({ todo, s, e })
      })
      items.sort((a, b) => a.s - b.s)
      const range = this.ganttRange
      return items.map((it, index) => {
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
    // 甘特横轴刻度（5 等分）
    ganttTicks() {
      const range = this.ganttRange
      if (!range) return []
      const ticks = []
      for (let i = 0; i <= 4; i++) {
        const d = new Date(range.start + range.span * i / 4)
        ticks.push({
          left: i * 25,
          label: (d.getMonth() + 1) + '月' + d.getDate() + '日'
        })
      }
      return ticks
    },
    // “今天”竖线位置，不在时间范围内则不显示
    ganttTodayLeft() {
      const range = this.ganttRange
      if (!range) return null
      const now = Date.now()
      if (now < range.start || now > range.end) return null
      return (now - range.start) / range.span * 100
    },
    // 时间节点事件流：开始/截止混排，按时间先后排列
    milestoneEvents() {
      const events = []
      this.timelineTodos.forEach(todo => {
        events.push({ todo, type: 'start', time: String(todo.startTime) })
        events.push({ todo, type: 'end', time: String(todo.endTime) })
      })
      events.sort((a, b) => {
        const diff = this.parseLocal(a.time).getTime() - this.parseLocal(b.time).getTime()
        if (diff !== 0) return diff
        if (a.type !== b.type) return a.type === 'start' ? -1 : 1
        return String(a.todo.title).localeCompare(String(b.todo.title))
      })
      const pad = n => String(n).padStart(2, '0')
      return events.map(ev => {
        const d = this.parseLocal(ev.time)
        return Object.assign({}, ev, {
          dateText: d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()),
          shortDate: (d.getMonth() + 1) + '月' + d.getDate() + '号',
          timeText: pad(d.getHours()) + ':' + pad(d.getMinutes())
        })
      })
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
    // ===== 视图切换 =====
    toggleCalendarView() {
      this.viewMode = this.viewMode === 'calendar' ? 'list' : 'calendar'
    },
    toggleTimelineView() {
      this.viewMode = this.viewMode === 'timeline' ? 'list' : 'timeline'
    },
    // ===== 日历月份切换（12 月右切进入下一年，1 月左切回到上一年）=====
    prevMonth() {
      if (this.calendarMonth === 0) {
        this.calendarMonth = 11
        this.calendarYear -= 1
      } else {
        this.calendarMonth -= 1
      }
    },
    nextMonth() {
      if (this.calendarMonth === 11) {
        this.calendarMonth = 0
        this.calendarYear += 1
      } else {
        this.calendarMonth += 1
      }
    },
    goToday() {
      const now = new Date()
      this.calendarYear = now.getFullYear()
      this.calendarMonth = now.getMonth()
    },
    formatDateKey(date) {
      const pad = n => String(n).padStart(2, '0')
      return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate())
    },
    // 日历任务条颜色分类：已完成绿 / 逾期红 / 进行中蓝
    chipClass(todo) {
      if (todo.completed || todo.status === '已完成') return 'done'
      if (todo.status === '逾期') return 'overdue'
      return 'doing'
    },
    // AI 预测当前新任务的完成成功率
    async predictSuccessRate() {
      if (!this.canPredict) {
        this.$message.warning('请先填写标题和开始/结束时间')
        return
      }
      this.predicting = true
      try {
        const res = await this.$http.post('/api/ai/predict', {
          title: this.form.title,
          content: this.form.content || '',
          label: this.form.label || null,
          startTime: this.form.startTime,
          endTime: this.form.endTime,
          daily: this.form.daily,
          top: this.form.top
        })
        if (res.data.code === 200) {
          this.prediction = res.data.data
        } else {
          this.$message.error(res.data.message || '预测失败')
        }
      } catch (err) {
        const msg = (err.response && err.response.data && err.response.data.message) || '预测失败，请稍后重试'
        this.$message.error(msg)
      } finally {
        this.predicting = false
      }
    },

    // 生成 AI 待办月报
    async generateTodoReport() {
      this.reportTitle = 'AI 待办月报'
      this.reportVisible = true
      this.generatingReport = true
      this.reportCached = false
      this.reportHtml = '<div style="color:#909399">AI 正在分析你近一个月的待办任务，请稍候...</div>'
      this.reportPeriod = ''
      try {
        const res = await this.$http.get('/api/ai/report/todo')
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

    // 生成 AI 待办周报
    async generateTodoWeeklyReport() {
      this.reportTitle = 'AI 待办周报'
      this.reportVisible = true
      this.generatingReport = true
      this.reportCached = false
      this.reportHtml = '<div style="color:#909399">AI 正在分析你近一周的待办任务，请稍候...</div>'
      this.reportPeriod = ''
      try {
        const res = await this.$http.get('/api/ai/report/todo/weekly')
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
      this.historyType = isWeekly ? 'todo-weekly' : 'todo-monthly'
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
      this.reportTitle = 'AI 待办' + (rep.reportType === 'todo-weekly' ? '周报' : '月报') + '（历史）'
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

    // 导出待办 CSV：blob 方式请求（自动携带 Token），从响应头解析文件名
    async exportCsv() {
      this.exporting = true
      try {
        const res = await this.$http.get('/api/todo/export/csv', { responseType: 'blob' })
        const disposition = res.headers['content-disposition'] || ''
        let filename = '待办导出.csv'
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
        this.$message.success('待办已导出为 CSV 文件')
      } catch (err) {
        if (!err.response || err.response.status !== 401) {
          this.$message.error('导出失败，请稍后重试')
        }
      } finally {
        this.exporting = false
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
        remindTime: this.form.remindTime || null,
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
.predict-box {
  margin-top: 12px;
  padding: 12px 14px;
  background: #fdf6ec;
  border: 1px solid #f5dab1;
  border-radius: 6px;
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

.remind-tip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--el-color-warning);
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

/* ===== 日历模式 ===== */
.calendar-view {
  margin-bottom: 26px;
}
.calendar-card {
  background: var(--el-bg-color);
  border-radius: 10px;
  padding: 18px;
  box-shadow: var(--el-box-shadow-light);
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
.legend-tip {
  margin-left: auto;
  color: var(--el-text-color-placeholder);
}

/* ===== 时间轴模式 ===== */
.timeline-view {
  margin-bottom: 26px;
}
.gantt-card,
.milestone-card {
  background: var(--el-bg-color);
  border-radius: 10px;
  padding: 18px 20px;
  box-shadow: var(--el-box-shadow-light);
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
  background: var(--el-bg-color);
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
