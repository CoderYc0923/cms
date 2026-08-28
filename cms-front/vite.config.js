import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'

/**
 * 双入口只做两件事：
 * 1) 按 mode 选 HTML 入口
 * 2) 按 mode 选 outDir
 * Antd / 公共组件按需：靠 Components resolver + 路由懒加载，不要手写 optimizeDeps。
 */
function resolveEntryHtml (appTarget) {
  return appTarget === 'docs'
    ? path.resolve(__dirname, 'docs.html')
    : path.resolve(__dirname, 'index.html')
}

function resolveOutDir (appTarget, docsSpace, env) {
  if (appTarget === 'docs') {
    if (env.VITE_DOCS_EMBED_AUTO === 'true') {
      return `dist/docs-embed-auto-${docsSpace || 'unknown'}`
    }
    if (env.VITE_DOCS_EMBED === 'true') {
      return `dist/docs-embed-${docsSpace || 'unknown'}`
    }
    return `dist/docs-${docsSpace || 'unknown'}`
  }
  return 'dist/admin'
}

/** 开发时把 / 指到当前入口的 html，避免 MPA 下根路径 404 */
function mpaDevFallbackPlugin (appTarget) {
  return {
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
  }
}

export default defineConfig(({ mode, command }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const appTarget = env.VITE_APP_TARGET || 'admin'
  const docsSpace = env.VITE_DOCS_SPACE || 'unknown'
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
        // 不自动扫业务目录：共享组件保持显式 import，随路由 chunk 按需加载
        dirs: [],
        resolvers: [
          AntDesignVueResolver({
            // ant-design-vue v4 = CSS-in-JS，不要拉 less/css 样式入口
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
      outDir: resolveOutDir(appTarget, docsSpace, env),
      emptyOutDir: true,
      rollupOptions: {
        input: resolveEntryHtml(appTarget),
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
          target: 'http://127.0.0.1:8080',
          ws: false,
          changeOrigin: true
        }
      }
    }
  }
})
