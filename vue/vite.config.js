import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    // 开发代理：/api 开头的请求转发到 Spring Boot（8080）
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    // 单文件超过 500KB 时提示分包，这里显式拆分后调高阈值减少噪音
    chunkSizeWarningLimit: 900,
    rollupOptions: {
      output: {
        /**
         * 手动分包：
         *  - echarts 只在数据看板用，单独成块，不进首屏
         *  - element-plus / vue 全家桶等第三方依赖单独成块，便于浏览器缓存
         */
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          if (id.includes('echarts') || id.includes('zrender')) return 'vendor-echarts'
          if (id.includes('element-plus') || id.includes('@element-plus')) return 'vendor-element'
          if (id.includes('/vue/') || id.includes('vue-router') || id.includes('vuex') || id.includes('@vue')) {
            return 'vendor-vue'
          }
          return 'vendor'
        }
      }
    }
  }
})
