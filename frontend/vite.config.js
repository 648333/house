import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue({
      template: {
        compilerOptions: {
          isCustomElement: (tag) => tag === 'model-viewer',
        },
      },
    }),
  ],
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return
          }

          if (id.includes('element-plus')) {
            return 'element-plus'
          }

          if (id.includes('vue') || id.includes('pinia') || id.includes('vue-router')) {
            return 'vue-core'
          }

          if (id.includes('axios')) {
            return 'axios'
          }

          if (id.includes('leaflet')) {
            return 'leaflet'
          }

          if (
            id.includes('@google/model-viewer')
            || id.includes('/model-viewer/')
            || id.includes('\\model-viewer\\')
            || id.includes('/lit/')
            || id.includes('\\lit\\')
          ) {
            return 'model-viewer'
          }
        }
      }
    }
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '127.0.0.1',
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      },
      // Some backend endpoints might not have the /api prefix in the frontend code
      // We also proxy requests like /auth or /properties directly
      '^/(auth|properties|users|appointments|messages|reviews|stats|requirements|support-tickets|upload|uploads)': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
