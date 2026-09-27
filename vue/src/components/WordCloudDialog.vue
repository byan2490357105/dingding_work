<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    width="640px"
    top="10vh"
    @update:model-value="emitVisible"
    @open="load"
  >
    <div v-loading="loading">
      <el-empty v-if="!loading && words.length === 0" description="暂无可用于生成词云的内容" :image-size="80" />
      <div v-else class="word-cloud">
        <span
          v-for="w in words"
          :key="w.name"
          class="wc-word"
          :style="{ fontSize: wordSize(w.value) + 'px', color: wcColor(w.value) }"
          :title="'出现 ' + w.value + ' 次'"
        >{{ w.name }}</span>
      </div>
      <div class="wc-tip">{{ tip }}</div>
    </div>
  </el-dialog>
</template>

<script>
import { notesApi } from '../api/notes'
import { todoApi } from '../api/todo'
import { notifyError } from '../utils/notify'

/**
 * 词云弹窗（Notes / Todo 共用）。
 * 打开时按 kind 自动向对应接口取词频数据，父组件只需 v-model 控制显隐。
 *
 * 用法：<word-cloud-dialog v-model="visible" kind="note" />
 */
export default {
  name: 'WordCloudDialog',
  props: {
    modelValue: {
      type: Boolean,
      default: false
    },
    /** note=笔记词云 / todo=待办词云 */
    kind: {
      type: String,
      default: 'note'
    },
    /** 取词数量 */
    limit: {
      type: Number,
      default: 50
    }
  },
  emits: ['update:modelValue'],
  data() {
    return {
      loading: false,
      words: []
    }
  },
  computed: {
    title() {
      return this.kind === 'todo' ? '待办词云' : '笔记词云'
    },
    tip() {
      const source = this.kind === 'todo' ? '待办标题与正文' : '笔记标题与正文'
      return `基于所有${source}的高频词生成，字号越大出现频次越高`
    },
    maxValue() {
      return this.words.length ? this.words[0].value : 1
    }
  },
  methods: {
    emitVisible(value) {
      this.$emit('update:modelValue', value)
    },
    async load() {
      this.loading = true
      try {
        const api = this.kind === 'todo' ? todoApi : notesApi
        this.words = (await api.wordCloud(this.limit)) || []
      } catch (err) {
        notifyError(err, '词云生成失败，请稍后重试')
      } finally {
        this.loading = false
      }
    },
    // 字号：频率越高字号越大（12px ~ 36px）
    wordSize(value) {
      if (!this.maxValue) return 14
      return Math.round(12 + (value / this.maxValue) * 24)
    },
    // 颜色：高频偏暖色，低频偏冷色
    wcColor(value) {
      const ratio = this.maxValue ? value / this.maxValue : 0
      if (ratio > 0.66) return '#e6a23c'
      if (ratio > 0.33) return '#409eff'
      return '#909399'
    }
  }
}
</script>

<style scoped>
.word-cloud {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 12px 16px;
  padding: 24px 12px;
  min-height: 240px;
  background: linear-gradient(135deg, #f5f7fa 0%, #ecf5ff 100%);
  border-radius: 8px;
}
.wc-word {
  cursor: default;
  font-weight: 600;
  display: inline-block;
  transition: transform 0.15s;
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
