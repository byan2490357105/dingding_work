<template>
  <div class="dashboard-page">
    <h2 class="page-title">数据看板</h2>

    <!-- 顶部统计卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card stat-notes">
          <div class="stat-icon"><el-icon><Document /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">总笔记</div>
            <div class="stat-value">{{ stats.totalNotes }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card stat-todos">
          <div class="stat-icon"><el-icon><List /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">总待办</div>
            <div class="stat-value">{{ stats.totalTodos }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card stat-rate">
          <div class="stat-icon"><el-icon><CircleCheck /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">完成率</div>
            <div class="stat-value">{{ stats.completionRate }}%</div>
            <div class="stat-sub">已完成 {{ stats.completedTodos }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card stat-month">
          <div class="stat-icon"><el-icon><Calendar /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">本月笔记</div>
            <div class="stat-value">{{ stats.monthNotes }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 待办完成趋势 -->
    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">近 30 天待办完成趋势</span>
          <span class="card-sub">按创建日期统计</span>
        </div>
      </template>
      <div v-loading="loading" class="chart-wrap">
        <v-chart class="chart" :option="trendOption" autoresize />
      </div>
    </el-card>

    <!-- 笔记产出热力图 -->
    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">近一年笔记产出热力图</span>
          <span class="card-sub">颜色越深表示当天笔记数越多</span>
        </div>
      </template>
      <div v-loading="loading" class="chart-wrap heatmap-wrap">
        <v-chart class="chart heatmap" :option="heatmapOption" autoresize />
      </div>
    </el-card>

    <!-- 标签分布 -->
    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">笔记标签分布</span>
            </div>
          </template>
          <div v-loading="loading" class="chart-wrap pie-wrap">
            <v-chart class="chart pie" :option="noteTagOption" autoresize />
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">待办标签分布</span>
            </div>
          </template>
          <div v-loading="loading" class="chart-wrap pie-wrap">
            <v-chart class="chart pie" :option="todoTagOption" autoresize />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { Document, List, CircleCheck, Calendar } from '@element-plus/icons-vue'
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart, HeatmapChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  CalendarComponent,
  VisualMapComponent,
  TitleComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

echarts.use([
  CanvasRenderer,
  LineChart,
  PieChart,
  HeatmapChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  CalendarComponent,
  VisualMapComponent,
  TitleComponent
])

export default {
  name: 'Dashboard',
  components: { VChart, Document, List, CircleCheck, Calendar },
  data() {
    return {
      loading: false,
      stats: {
        totalNotes: 0,
        totalTodos: 0,
        completedTodos: 0,
        completionRate: 0,
        monthNotes: 0,
        todoCompletionTrend: [],
        noteHeatmap: [],
        tagDistribution: { notes: [], todos: [] }
      }
    }
  },
  computed: {
    // 近 30 天待办完成趋势折线图
    trendOption() {
      const data = this.stats.todoCompletionTrend || []
      const dates = data.map(p => p.date)
      const totals = data.map(p => Number(p.total) || 0)
      const completed = data.map(p => Number(p.completed) || 0)
      return {
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          data: ['总数', '已完成'],
          top: 0
        },
        grid: {
          left: 40,
          right: 24,
          top: 40,
          bottom: 40
        },
        xAxis: {
          type: 'category',
          data: dates,
          axisLabel: {
            formatter: v => v.slice(5)
          },
          axisTick: { alignWithLabel: true }
        },
        yAxis: {
          type: 'value',
          minInterval: 1
        },
        series: [
          {
            name: '总数',
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            itemStyle: { color: '#409EFF' },
            areaStyle: { opacity: 0.12 },
            data: totals
          },
          {
            name: '已完成',
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            itemStyle: { color: '#67C23A' },
            areaStyle: { opacity: 0.12 },
            data: completed
          }
        ]
      }
    },
    // 近一年笔记日历热力图
    heatmapOption() {
      const data = this.stats.noteHeatmap || []
      const seriesData = data.map(p => [p.date, Number(p.count) || 0])
      const year = new Date().getFullYear()
      return {
        tooltip: {
          formatter: p => `${p.value[0]}<br/>笔记数：${p.value[1]}`
        },
        visualMap: {
          min: 0,
          max: Math.max(5, ...seriesData.map(d => d[1])),
          calculable: true,
          orient: 'horizontal',
          left: 'center',
          bottom: 0,
          inRange: {
            color: ['#ebedf0', '#c6e48b', '#7bc96f', '#239a3b', '#196127']
          }
        },
        calendar: {
          top: 40,
          left: 40,
          right: 30,
          cellSize: ['auto', 16],
          range: String(year),
          itemStyle: {
            borderWidth: 1.5,
            borderColor: '#fff',
            color: '#ebedf0'
          },
          yearLabel: { show: false },
          dayLabel: {
            firstDay: 1,
            nameMap: 'cn'
          },
          monthLabel: {
            nameMap: 'cn'
          },
          splitLine: { show: false }
        },
        series: [
          {
            type: 'heatmap',
            coordinateSystem: 'calendar',
            data: seriesData
          }
        ]
      }
    },
    // 笔记标签分布饼图
    noteTagOption() {
      return this.buildPieOption(this.stats.tagDistribution.notes || [], '暂无笔记标签数据')
    },
    // 待办标签分布饼图
    todoTagOption() {
      return this.buildPieOption(this.stats.tagDistribution.todos || [], '暂无待办标签数据')
    }
  },
  created() {
    this.fetchDashboard()
  },
  methods: {
    async fetchDashboard() {
      this.loading = true
      try {
        const res = await this.$http.get('/api/stats/dashboard')
        if (res.data.code === 200 && res.data.data) {
          const data = res.data.data
          this.stats = {
            totalNotes: data.totalNotes || 0,
            totalTodos: data.totalTodos || 0,
            completedTodos: data.completedTodos || 0,
            completionRate: data.completionRate || 0,
            monthNotes: data.monthNotes || 0,
            todoCompletionTrend: data.todoCompletionTrend || [],
            noteHeatmap: data.noteHeatmap || [],
            tagDistribution: data.tagDistribution || { notes: [], todos: [] }
          }
        } else {
          this.$message.error(res.data.message || '加载仪表盘失败')
        }
      } catch (err) {
        if (!err.response || err.response.status !== 401) {
          this.$message.error('加载仪表盘失败')
        }
      } finally {
        this.loading = false
      }
    },
    buildPieOption(list, emptyText) {
      const data = list.map(item => ({
        name: item.tag,
        value: Number(item.count) || 0
      }))
      return {
        title: data.length === 0 ? {
          text: emptyText,
          left: 'center',
          top: 'center',
          textStyle: { color: '#909399', fontSize: 14, fontWeight: 'normal' }
        } : undefined,
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 10,
          top: 'middle'
        },
        series: [
          {
            type: 'pie',
            radius: ['40%', '70%'],
            center: ['60%', '50%'],
            avoidLabelOverlap: true,
            itemStyle: {
              borderRadius: 6,
              borderColor: '#fff',
              borderWidth: 2
            },
            label: {
              show: true,
              formatter: '{b}: {d}%'
            },
            data
          }
        ],
        color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#9254DE', '#13C2C2', '#EB2F96', '#FAAD14', '#2F54EB', '#7CB305']
      }
    }
  }
}
</script>

<style scoped>
.dashboard-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 20px;
  text-align: left;
}
.page-title {
  margin: 0 0 18px;
  font-size: 22px;
  font-weight: 700;
  color: #303133;
}
.stat-cards {
  margin-bottom: 18px;
}
.stat-card {
  border-radius: 10px;
  border: none;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #fff 0%, #f7fafc 100%);
}
.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 18px 20px;
}
.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
  flex-shrink: 0;
}
.stat-notes .stat-icon { background: linear-gradient(135deg, #409EFF, #66b1ff); }
.stat-todos .stat-icon { background: linear-gradient(135deg, #E6A23C, #f0c78a); }
.stat-rate .stat-icon { background: linear-gradient(135deg, #67C23A, #95d475); }
.stat-month .stat-icon { background: linear-gradient(135deg, #9254DE, #b37feb); }
.stat-info {
  flex: 1;
  min-width: 0;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}
.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}
.stat-sub {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
.chart-card {
  margin-bottom: 18px;
  border-radius: 10px;
}
.card-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.card-sub {
  font-size: 12px;
  color: #909399;
}
.chart-wrap {
  width: 100%;
}
.chart {
  width: 100%;
  height: 320px;
}
.chart.heatmap {
  height: 220px;
}
.chart.pie {
  height: 320px;
}
</style>
