<template>
  <div class="rich-editor">
    <div class="rich-editor__toolbar">
      <button type="button" title="加粗" @mousedown.prevent="exec('bold')"><strong>B</strong></button>
      <button type="button" title="斜体" @mousedown.prevent="exec('italic')"><em>I</em></button>
      <button type="button" title="下划线" @mousedown.prevent="exec('underline')"><u>U</u></button>
      <button type="button" title="删除线" @mousedown.prevent="exec('strikeThrough')"><s>S</s></button>
      <span class="divider"></span>
      <button type="button" title="无序列表" @mousedown.prevent="exec('insertUnorderedList')">• 列表</button>
      <button type="button" title="有序列表" @mousedown.prevent="exec('insertOrderedList')">1. 列表</button>
      <button type="button" title="引用" @mousedown.prevent="exec('formatBlock', 'blockquote')">引用</button>
      <span class="divider"></span>
      <button type="button" title="清除格式" @mousedown.prevent="exec('removeFormat')">清除格式</button>
    </div>
    <div
      ref="editor"
      class="rich-editor__content"
      contenteditable="true"
      :data-placeholder="placeholder"
      @input="emitContent"
    ></div>
  </div>
</template>

<script>
export default {
  name: 'RichEditor',
  props: {
    modelValue: {
      type: String,
      default: ''
    },
    placeholder: {
      type: String,
      default: '请输入内容...'
    }
  },
  emits: ['update:modelValue'],
  watch: {
    modelValue(value) {
      const editor = this.$refs.editor
      if (editor && value !== editor.innerHTML) {
        editor.innerHTML = value || ''
      }
    }
  },
  mounted() {
    this.$refs.editor.innerHTML = this.modelValue || ''
  },
  methods: {
    exec(command, value = null) {
      const editor = this.$refs.editor
      if (!editor) return
      editor.focus()
      document.execCommand(command, false, value)
      this.emitContent()
    },
    emitContent() {
      const html = this.$refs.editor.innerHTML
      if (html !== this.modelValue) {
        this.$emit('update:modelValue', html)
      }
    }
  }
}
</script>

<style scoped>
.rich-editor {
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  overflow: hidden;
}

.rich-editor__toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 2px;
  padding: 6px;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color);
}

.rich-editor__toolbar button {
  min-width: 30px;
  height: 28px;
  padding: 0 8px;
  border: none;
  border-radius: 3px;
  background: transparent;
  cursor: pointer;
  color: var(--el-text-color-primary);
}

.rich-editor__toolbar button:hover {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.rich-editor__toolbar .divider {
  width: 1px;
  height: 18px;
  margin: 0 4px;
  background: var(--el-border-color);
}

.rich-editor__content {
  min-height: 180px;
  max-height: 420px;
  overflow-y: auto;
  padding: 10px 12px;
  outline: none;
  line-height: 1.6;
  text-align: left;
}

.rich-editor__content:empty::before {
  content: attr(data-placeholder);
  color: var(--el-text-color-placeholder);
}
</style>
