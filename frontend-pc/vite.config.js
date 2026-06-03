import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  build: {
    outDir: path.resolve(__dirname, '../backend/src/main/resources/static'),
    emptyOutDir: true
  },
  server: {
    port: 5174,
    proxy: {
      '/api/v1/investment': {
        target: 'http://localhost:8000',
        changeOrigin: true
      },
      '/api/v1/spatial': {
        target: 'http://localhost:8000',
        changeOrigin: true
      },
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/auth': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/manage': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
