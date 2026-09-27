import { onBeforeUnmount, ref } from 'vue'

/**
 * 搜索关键字防抖。
 * 用法：
 *   const { keyword, keywordInput } = useSearchDebounce(300, () => { page = 1; fetch() })
 *   <el-input v-model="keywordInput" />
 */
export function useSearchDebounce(delay, onChange) {
  let timer = null
  const keyword = ref('')

  const cancel = () => {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
  }

  const schedule = value => {
    keyword.value = value
    cancel()
    timer = setTimeout(() => {
      timer = null
      onChange(value)
    }, delay)
  }

  onBeforeUnmount(cancel)

  return { keyword, schedule, cancel }
}

/** 普通防抖函数（不绑定关键字状态） */
export function debounce(fn, delay = 300) {
  let timer = null
  const wrapped = (...args) => {
    clearTimeout(timer)
    timer = setTimeout(() => fn(...args), delay)
  }
  wrapped.cancel = () => clearTimeout(timer)
  return wrapped
}
