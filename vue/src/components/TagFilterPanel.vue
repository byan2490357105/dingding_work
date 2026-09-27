<template>
  <div class="tag-filter-panel" v-if="visible && stats.length > 0">
    <div class="tag-filter-head" @click="toggleCollapse">
      <el-icon class="collapse-arrow" :class="{ collapsed }"><ArrowDown /></el-icon>
      <span class="tag-filter-title">{{ hint }}</span>
      <el-tag size="small" type="info">{{ selected.length }}/{{ stats.length }} 个标签</el-tag>
    </div>

    <div class="tag-filter-body" v-show="!collapsed">
      <div class="tag-filter-toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索标签"
          size="small"
          clearable
          style="width: 200px"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button size="small" link @click="selectAll">全选</el-button>
        <el-button size="small" link @click="selectNone">全不选</el-button>
      </div>

      <div class="tag-carousel">
        <el-button
          v-if="totalPages > 1"
          link
          :disabled="page === 0"
          class="tag-nav-btn"
          @click="prevPage"
        >
          <el-icon><ArrowLeft /></el-icon>
        </el-button>

        <transition :name="slideDir === 'next' ? 'tag-slide-next' : 'tag-slide-prev'" mode="out-in">
          <div :key="page" class="tag-page">
            <el-checkbox-group :model-value="selected" class="tag-checkbox-row" @update:model-value="emitSelected">
              <el-checkbox
                v-for="item in pagedItems"
                :key="item.name"
                :label="item.name"
                class="tag-checkbox-item"
              >
                <el-tag size="small" effect="plain">{{ item.name }}</el-tag>
                <span class="tag-count">{{ item.count }}</span>
              </el-checkbox>
            </el-checkbox-group>
          </div>
        </transition>

        <el-button
          v-if="totalPages > 1"
          link
          :disabled="page >= totalPages - 1"
          class="tag-nav-btn"
          @click="nextPage"
        >
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { ArrowDown, ArrowLeft, ArrowRight, Search } from '@element-plus/icons-vue'

/**
 * 通用标签筛选面板（Notes / Todo 共用）。
 *
 * 支持：折叠、搜索标签、全选/全不选、每页 5 个的轮播翻页，
 * 以及「首次加载默认全选 / 后续只自动勾选新出现的标签」的既有交互。
 *
 * 用法：
 *   <tag-filter-panel v-model="selectedTags" v-model:collapsed="collapsed" :stats="tagStats" />
 *   stats 形如 [{ name: '学习', count: 3 }]
 */
export default {
  name: 'TagFilterPanel',
  components: { ArrowDown, ArrowLeft, ArrowRight, Search },
  props: {
    /** 已勾选的标签名数组 */
    modelValue: {
      type: Array,
      default: () => []
    },
    /** 全部标签统计 [{ name, count }] */
    stats: {
      type: Array,
      default: () => []
    },
    /** 面板提示文案 */
    hint: {
      type: String,
      default: '按标签筛选（勾选显示，取消勾选则隐藏该标签的内容）'
    },
    /** 是否显示（按视图模式控制） */
    visible: {
      type: Boolean,
      default: true
    },
    /** 折叠状态（.sync / v-model:collapsed） */
    collapsed: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue', 'update:collapsed'],
  data() {
    return {
      keyword: '',
      page: 0,
      slideDir: 'next'
    }
  },
  computed: {
    selected() {
      return this.modelValue || []
    },
    visibleItems() {
      if (!this.keyword) return this.stats
      const kw = this.keyword.toLowerCase()
      return this.stats.filter(item => item.name.toLowerCase().includes(kw))
    },
    totalPages() {
      return Math.max(1, Math.ceil(this.visibleItems.length / 5))
    },
    pagedItems() {
      const start = this.page * 5
      return this.visibleItems.slice(start, start + 5)
    }
  },
  watch: {
    // 搜索标签时回到第一页
    keyword() {
      this.page = 0
    },
    // 标签统计变化：首次默认全选；之后只把新出现的标签自动勾上
    stats(newStats, oldStats) {
      const names = newStats.map(item => item.name)
      if (!oldStats || oldStats.length === 0) {
        this.emitSelected(names.slice())
        return
      }
      const oldNames = oldStats.map(item => item.name)
      const added = names.filter(name => !oldNames.includes(name))
      if (added.length) {
        this.emitSelected([...this.selected, ...added])
      }
    }
  },
  methods: {
    toggleCollapse() {
      this.$emit('update:collapsed', !this.collapsed)
    },
    emitSelected(value) {
      this.$emit('update:modelValue', value)
    },
    selectAll() {
      this.emitSelected(this.stats.map(item => item.name))
    },
    selectNone() {
      this.emitSelected([])
    },
    prevPage() {
      if (this.page > 0) {
        this.slideDir = 'prev'
        this.page--
      }
    },
    nextPage() {
      if (this.page < this.totalPages - 1) {
        this.slideDir = 'next'
        this.page++
      }
    }
  }
}
</script>

<style scoped>
.tag-filter-panel {
  background: #fff;
  border: 1px solid var(--wb-border, #ebeef5);
  border-radius: 10px;
  margin-bottom: 16px;
  overflow: hidden;
}
.tag-filter-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  cursor: pointer;
  background: #f7f9fc;
  user-select: none;
}
.tag-filter-head:hover {
  background: #eef5ff;
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
.tag-count {
  font-size: 12px;
  color: #909399;
  margin-left: 4px;
}
.tag-slide-next-enter-active,
.tag-slide-next-leave-active,
.tag-slide-prev-enter-active,
.tag-slide-prev-leave-active {
  transition: transform 0.3s ease;
}
.tag-slide-next-enter-from { transform: translateX(100%); }
.tag-slide-next-leave-to { transform: translateX(-100%); }
.tag-slide-prev-enter-from { transform: translateX(-100%); }
.tag-slide-prev-leave-to { transform: translateX(100%); }
</style>
