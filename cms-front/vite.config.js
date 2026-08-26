import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'

const resolveEntryHtml = (appTarget) =>
  appTarget === 'docs'
    ? path.resolve(__dirname, 'docs.html')
    : path.resolve(__dirname, 'index.html')

const resolveOutDir = (appTarget, docsSpace) => {
  if (appTarget === 'docs') {
    return `dist/docs-${docsSpace || 'unknown'}`
  }
  return 'dist/admin'
}

const mpaDevFallbackPlugin = (appTarget) => ({
  name: 'mpa-dev-fallback',
  configureServer (server) {
    server.middlewares.use((req, _res, next) => {
      const url = req.url?.split('?')[0] ?? ''
      if (appTarget === 'docs' && (url === '/' || url === '/index.html')) {
        req.url = '/docs.html'
      }
      if (appTarget === 'admin' && url === '/') {
        req.url = '/index.html'
      }
      next()
    })
  }
})

// https://vitejs.dev/config/
export default defineConfig(({ mode, command }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const appTarget = env.VITE_APP_TARGET || 'admin'
  const docsSpace = env.VITE_DOCS_SPACE || 'unknown'
  const entryHtml = resolveEntryHtml(appTarget)
  const isDev = command === 'serve'

  return {
    plugins: [
      vue(),
      isDev && mpaDevFallbackPlugin(appTarget),
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
      }),
      Components({
        // 模板里的 a-xxx 按需解析；JS API（message 等）仍显式 import
        resolvers: [
          AntDesignVueResolver({
            // ant-design-vue v4 为 CSS-in-JS，不要再拉 less 样式入口
            importStyle: false
          })
        ],
        dts: './components.d.ts'
      })
    ].filter(Boolean),
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src')
      }
    },
    define: {
      'process.env': env
    },
    build: {
      sourcemap: command === 'build' ? false : 'inline',
      outDir: resolveOutDir(appTarget, docsSpace),
      emptyOutDir: true,
      rollupOptions: {
        input: entryHtml,
        output: {
          chunkFileNames: 'static/js/[name]-[hash].js',
          entryFileNames: 'static/js/[name]-[hash].js',
          assetFileNames: 'static/[ext]/[name]-[hash].[ext]'
        }
      }
    },
    server: {
      host: '0.0.0.0',
      open: false,
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          ws: false,
          changeOrigin: true
        }
      }
    }
  }
})
