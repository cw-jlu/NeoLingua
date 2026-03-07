import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { VantResolver } from '@vant/auto-import-resolver'
import path from 'path'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({ resolvers: [VantResolver()] }),
    Components({ resolvers: [VantResolver()] }),
  ],
  resolve: {
    alias: { '@': path.resolve(__dirname, 'src') },
  },
  server: {
    port: 3001,
    proxy: {
      // ai_service (Python, 端口 8089) 的直连路由：TTS
      '/api/ai/tts': {
        target: 'http://localhost:8089',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      // 其余请求走 api_gateway（含 /user/analysis/audio/upload）
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
})
