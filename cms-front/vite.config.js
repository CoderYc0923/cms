import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import AutoImport from 'unplugin-auto-import/vite'



// https://vitejs.dev/config/
export default defineConfig(({ mode, command }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [
      vue(),
      AutoImport({
        include: [/\.vue$/, /\.vue\?vue/, /\.js$/],
        imports: ['vue', 'vue-router', 'pinia'],
        eslintrc: {
          enabled: true,
          filepath: './.eslintrc-auto-import.json',
          globalsPropValue: true
        },
        dts: './auto-imports.d.ts',
        dirs: ['./src/hooks', './src/stores']
      })
    ],
    resolve: {
      alias: {
        // eslint-disable-next-line no-undef
        '@': path.resolve(__dirname, './src')
      }
    },
    define: {
      'process.env': env
    },
    build: {
      sourcemap: command === 'build' ? false : 'inline',
      outDir: 'dist', //指定输出目录
      // 将js、css文件分离到单独文件夹
      rollupOptions: {
        output: {
          chunkFileNames: 'static/js/[name]-[hash].js',
          entryFileNames: 'static/js/[name]-[hash].js',
          assetFileNames: 'static/[ext]/[name]-[hash].[ext]'
        }
      }
    },
    server: {
      host: '0.0.0.0',
      proxy: { 
        '/api': {
          target: 'http://localhost:8080', // cms-back-admin 默认端口
          // target: 'http://localhost:3000', // 旧 mock
          ws: false,
          changeOrigin: true
        }
      }
    }
  }
})
