import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          if (id.includes('echarts')) return 'vendor-echarts'
          if (id.includes('element-plus')) return 'vendor-element-plus'
          if (id.includes('vue') || id.includes('pinia') || id.includes('vue-router')) return 'vendor-vue'
          return 'vendor'
        }
      }
    },
    chunkSizeWarningLimit: 900
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
