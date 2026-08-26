// vite.config.js
import { defineConfig, loadEnv } from "file:///D:/code-self/cms/cms-front/node_modules/.pnpm/vite@4.5.14_less@4.4.1/node_modules/vite/dist/node/index.js";
import vue from "file:///D:/code-self/cms/cms-front/node_modules/.pnpm/@vitejs+plugin-vue@4.6.2_vite@4.5.14_less@4.4.1__vue@3.5.21/node_modules/@vitejs/plugin-vue/dist/index.mjs";
import path from "path";
import AutoImport from "file:///D:/code-self/cms/cms-front/node_modules/.pnpm/unplugin-auto-import@0.17.8_f67cbbc4e8b39ce25e6a9aa9a3d454ca/node_modules/unplugin-auto-import/dist/vite.js";
var __vite_injected_original_dirname = "D:\\code-self\\cms\\cms-front";
var resolveEntryHtml = (appTarget) => appTarget === "docs" ? path.resolve(__vite_injected_original_dirname, "docs.html") : path.resolve(__vite_injected_original_dirname, "index.html");
var resolveOutDir = (appTarget, docsSpace) => {
  if (appTarget === "docs") {
    return `dist/docs-${docsSpace || "unknown"}`;
  }
  return "dist/admin";
};
var mpaDevFallbackPlugin = (appTarget) => ({
  name: "mpa-dev-fallback",
  configureServer(server) {
    server.middlewares.use((req, _res, next) => {
      var _a;
      const url = ((_a = req.url) == null ? void 0 : _a.split("?")[0]) ?? "";
      if (appTarget === "docs" && (url === "/" || url === "/index.html")) {
        req.url = "/docs.html";
      }
      if (appTarget === "admin" && url === "/") {
        req.url = "/index.html";
      }
      next();
    });
  }
});
var vite_config_default = defineConfig(({ mode, command }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const appTarget = env.VITE_APP_TARGET || "admin";
  const docsSpace = env.VITE_DOCS_SPACE || "unknown";
  const entryHtml = resolveEntryHtml(appTarget);
  const isDev = command === "serve";
  return {
    plugins: [
      vue(),
      isDev && mpaDevFallbackPlugin(appTarget),
      AutoImport({
        include: [/\.vue$/, /\.vue\?vue/, /\.js$/],
        imports: ["vue", "vue-router", "pinia"],
        eslintrc: {
          enabled: true,
          filepath: "./.eslintrc-auto-import.json",
          globalsPropValue: true
        },
        dts: "./auto-imports.d.ts",
        dirs: ["./src/hooks", "./src/stores"]
      })
    ].filter(Boolean),
    resolve: {
      alias: {
        "@": path.resolve(__vite_injected_original_dirname, "./src")
      }
    },
    define: {
      "process.env": env
    },
    build: {
      sourcemap: command === "build" ? false : "inline",
      outDir: resolveOutDir(appTarget, docsSpace),
      emptyOutDir: true,
      rollupOptions: {
        input: entryHtml,
        output: {
          chunkFileNames: "static/js/[name]-[hash].js",
          entryFileNames: "static/js/[name]-[hash].js",
          assetFileNames: "static/[ext]/[name]-[hash].[ext]"
        }
      }
    },
    // 只预构建轻量依赖。ant-design-vue / wangeditor 整包预构建在 Windows 上易 OOM
    // （esbuild "cannot allocate memory" → exit 3221226505）
    // dayjs 插件为 CJS：排除 ant-design-vue 后必须单独 include，否则浏览器报
    // "does not provide an export named 'default'"
    optimizeDeps: {
      include: [
        "vue",
        "vue-router",
        "pinia",
        "axios",
        "dayjs",
        "dayjs/locale/zh-cn",
        "dayjs/plugin/advancedFormat",
        "dayjs/plugin/customParseFormat",
        "dayjs/plugin/weekday",
        "dayjs/plugin/localeData",
        "dayjs/plugin/weekOfYear",
        "dayjs/plugin/weekYear",
        "dayjs/plugin/quarterOfYear",
        "lodash",
        "nprogress",
        "js-cookie",
        "qs"
      ],
      exclude: [
        "ant-design-vue",
        "@ant-design/icons-vue",
        "@wangeditor/editor",
        "@wangeditor/editor-for-vue"
      ]
    },
    server: {
      host: "0.0.0.0",
      // Windows 上 open:true 首次拉依赖时偶发 OOM/崩溃，改为手动打开浏览器
      open: false,
      proxy: {
        "/api": {
          target: "http://localhost:8080",
          ws: false,
          changeOrigin: true
        }
      }
    }
  };
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcuanMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCJEOlxcXFxjb2RlLXNlbGZcXFxcY21zXFxcXGNtcy1mcm9udFwiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9maWxlbmFtZSA9IFwiRDpcXFxcY29kZS1zZWxmXFxcXGNtc1xcXFxjbXMtZnJvbnRcXFxcdml0ZS5jb25maWcuanNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfaW1wb3J0X21ldGFfdXJsID0gXCJmaWxlOi8vL0Q6L2NvZGUtc2VsZi9jbXMvY21zLWZyb250L3ZpdGUuY29uZmlnLmpzXCI7aW1wb3J0IHsgZGVmaW5lQ29uZmlnLCBsb2FkRW52IH0gZnJvbSAndml0ZSdcclxuaW1wb3J0IHZ1ZSBmcm9tICdAdml0ZWpzL3BsdWdpbi12dWUnXHJcbmltcG9ydCBwYXRoIGZyb20gJ3BhdGgnXHJcbmltcG9ydCBBdXRvSW1wb3J0IGZyb20gJ3VucGx1Z2luLWF1dG8taW1wb3J0L3ZpdGUnXHJcblxyXG5jb25zdCByZXNvbHZlRW50cnlIdG1sID0gKGFwcFRhcmdldCkgPT5cclxuICBhcHBUYXJnZXQgPT09ICdkb2NzJ1xyXG4gICAgPyBwYXRoLnJlc29sdmUoX19kaXJuYW1lLCAnZG9jcy5odG1sJylcclxuICAgIDogcGF0aC5yZXNvbHZlKF9fZGlybmFtZSwgJ2luZGV4Lmh0bWwnKVxyXG5cclxuY29uc3QgcmVzb2x2ZU91dERpciA9IChhcHBUYXJnZXQsIGRvY3NTcGFjZSkgPT4ge1xyXG4gIGlmIChhcHBUYXJnZXQgPT09ICdkb2NzJykge1xyXG4gICAgcmV0dXJuIGBkaXN0L2RvY3MtJHtkb2NzU3BhY2UgfHwgJ3Vua25vd24nfWBcclxuICB9XHJcbiAgcmV0dXJuICdkaXN0L2FkbWluJ1xyXG59XHJcblxyXG5jb25zdCBtcGFEZXZGYWxsYmFja1BsdWdpbiA9IChhcHBUYXJnZXQpID0+ICh7XHJcbiAgbmFtZTogJ21wYS1kZXYtZmFsbGJhY2snLFxyXG4gIGNvbmZpZ3VyZVNlcnZlciAoc2VydmVyKSB7XHJcbiAgICBzZXJ2ZXIubWlkZGxld2FyZXMudXNlKChyZXEsIF9yZXMsIG5leHQpID0+IHtcclxuICAgICAgY29uc3QgdXJsID0gcmVxLnVybD8uc3BsaXQoJz8nKVswXSA/PyAnJ1xyXG4gICAgICBpZiAoYXBwVGFyZ2V0ID09PSAnZG9jcycgJiYgKHVybCA9PT0gJy8nIHx8IHVybCA9PT0gJy9pbmRleC5odG1sJykpIHtcclxuICAgICAgICByZXEudXJsID0gJy9kb2NzLmh0bWwnXHJcbiAgICAgIH1cclxuICAgICAgLy8gYWRtaW5cdUZGMUFcdTRGRERcdThCQzFcdTY4MzlcdThERUZcdTVGODRcdTg0M0RcdTUyMzAgaW5kZXguaHRtbFx1RkYwQ1x1OTA3Rlx1NTE0RFx1NEUyQVx1NTIyQlx1NzNBRlx1NTg4M1x1NEUwQiAvIFx1NzZGNFx1NjNBNSA0MDRcclxuICAgICAgaWYgKGFwcFRhcmdldCA9PT0gJ2FkbWluJyAmJiB1cmwgPT09ICcvJykge1xyXG4gICAgICAgIHJlcS51cmwgPSAnL2luZGV4Lmh0bWwnXHJcbiAgICAgIH1cclxuICAgICAgbmV4dCgpXHJcbiAgICB9KVxyXG4gIH1cclxufSlcclxuXHJcbi8vIGh0dHBzOi8vdml0ZWpzLmRldi9jb25maWcvXHJcbmV4cG9ydCBkZWZhdWx0IGRlZmluZUNvbmZpZygoeyBtb2RlLCBjb21tYW5kIH0pID0+IHtcclxuICBjb25zdCBlbnYgPSBsb2FkRW52KG1vZGUsIHByb2Nlc3MuY3dkKCksICcnKVxyXG4gIGNvbnN0IGFwcFRhcmdldCA9IGVudi5WSVRFX0FQUF9UQVJHRVQgfHwgJ2FkbWluJ1xyXG4gIGNvbnN0IGRvY3NTcGFjZSA9IGVudi5WSVRFX0RPQ1NfU1BBQ0UgfHwgJ3Vua25vd24nXHJcbiAgY29uc3QgZW50cnlIdG1sID0gcmVzb2x2ZUVudHJ5SHRtbChhcHBUYXJnZXQpXHJcbiAgY29uc3QgaXNEZXYgPSBjb21tYW5kID09PSAnc2VydmUnXHJcblxyXG4gIHJldHVybiB7XHJcbiAgICBwbHVnaW5zOiBbXHJcbiAgICAgIHZ1ZSgpLFxyXG4gICAgICBpc0RldiAmJiBtcGFEZXZGYWxsYmFja1BsdWdpbihhcHBUYXJnZXQpLFxyXG4gICAgICBBdXRvSW1wb3J0KHtcclxuICAgICAgICBpbmNsdWRlOiBbL1xcLnZ1ZSQvLCAvXFwudnVlXFw/dnVlLywgL1xcLmpzJC9dLFxyXG4gICAgICAgIGltcG9ydHM6IFsndnVlJywgJ3Z1ZS1yb3V0ZXInLCAncGluaWEnXSxcclxuICAgICAgICBlc2xpbnRyYzoge1xyXG4gICAgICAgICAgZW5hYmxlZDogdHJ1ZSxcclxuICAgICAgICAgIGZpbGVwYXRoOiAnLi8uZXNsaW50cmMtYXV0by1pbXBvcnQuanNvbicsXHJcbiAgICAgICAgICBnbG9iYWxzUHJvcFZhbHVlOiB0cnVlXHJcbiAgICAgICAgfSxcclxuICAgICAgICBkdHM6ICcuL2F1dG8taW1wb3J0cy5kLnRzJyxcclxuICAgICAgICBkaXJzOiBbJy4vc3JjL2hvb2tzJywgJy4vc3JjL3N0b3JlcyddXHJcbiAgICAgIH0pXHJcbiAgICBdLmZpbHRlcihCb29sZWFuKSxcclxuICAgIHJlc29sdmU6IHtcclxuICAgICAgYWxpYXM6IHtcclxuICAgICAgICAnQCc6IHBhdGgucmVzb2x2ZShfX2Rpcm5hbWUsICcuL3NyYycpXHJcbiAgICAgIH1cclxuICAgIH0sXHJcbiAgICBkZWZpbmU6IHtcclxuICAgICAgJ3Byb2Nlc3MuZW52JzogZW52XHJcbiAgICB9LFxyXG4gICAgYnVpbGQ6IHtcclxuICAgICAgc291cmNlbWFwOiBjb21tYW5kID09PSAnYnVpbGQnID8gZmFsc2UgOiAnaW5saW5lJyxcclxuICAgICAgb3V0RGlyOiByZXNvbHZlT3V0RGlyKGFwcFRhcmdldCwgZG9jc1NwYWNlKSxcclxuICAgICAgZW1wdHlPdXREaXI6IHRydWUsXHJcbiAgICAgIHJvbGx1cE9wdGlvbnM6IHtcclxuICAgICAgICBpbnB1dDogZW50cnlIdG1sLFxyXG4gICAgICAgIG91dHB1dDoge1xyXG4gICAgICAgICAgY2h1bmtGaWxlTmFtZXM6ICdzdGF0aWMvanMvW25hbWVdLVtoYXNoXS5qcycsXHJcbiAgICAgICAgICBlbnRyeUZpbGVOYW1lczogJ3N0YXRpYy9qcy9bbmFtZV0tW2hhc2hdLmpzJyxcclxuICAgICAgICAgIGFzc2V0RmlsZU5hbWVzOiAnc3RhdGljL1tleHRdL1tuYW1lXS1baGFzaF0uW2V4dF0nXHJcbiAgICAgICAgfVxyXG4gICAgICB9XHJcbiAgICB9LFxyXG4gICAgLy8gXHU1M0VBXHU5ODg0XHU2Nzg0XHU1RUZBXHU4RjdCXHU5MUNGXHU0RjlEXHU4RDU2XHUzMDAyYW50LWRlc2lnbi12dWUgLyB3YW5nZWRpdG9yIFx1NjU3NFx1NTMwNVx1OTg4NFx1Njc4NFx1NUVGQVx1NTcyOCBXaW5kb3dzIFx1NEUwQVx1NjYxMyBPT01cclxuICAgIC8vIFx1RkYwOGVzYnVpbGQgXCJjYW5ub3QgYWxsb2NhdGUgbWVtb3J5XCIgXHUyMTkyIGV4aXQgMzIyMTIyNjUwNVx1RkYwOVxyXG4gICAgLy8gZGF5anMgXHU2M0QyXHU0RUY2XHU0RTNBIENKU1x1RkYxQVx1NjM5Mlx1OTY2NCBhbnQtZGVzaWduLXZ1ZSBcdTU0MEVcdTVGQzVcdTk4N0JcdTUzNTVcdTcyRUMgaW5jbHVkZVx1RkYwQ1x1NTQyNlx1NTIxOVx1NkQ0Rlx1ODlDOFx1NTY2OFx1NjJBNVxyXG4gICAgLy8gXCJkb2VzIG5vdCBwcm92aWRlIGFuIGV4cG9ydCBuYW1lZCAnZGVmYXVsdCdcIlxyXG4gICAgb3B0aW1pemVEZXBzOiB7XHJcbiAgICAgIGluY2x1ZGU6IFtcclxuICAgICAgICAndnVlJyxcclxuICAgICAgICAndnVlLXJvdXRlcicsXHJcbiAgICAgICAgJ3BpbmlhJyxcclxuICAgICAgICAnYXhpb3MnLFxyXG4gICAgICAgICdkYXlqcycsXHJcbiAgICAgICAgJ2RheWpzL2xvY2FsZS96aC1jbicsXHJcbiAgICAgICAgJ2RheWpzL3BsdWdpbi9hZHZhbmNlZEZvcm1hdCcsXHJcbiAgICAgICAgJ2RheWpzL3BsdWdpbi9jdXN0b21QYXJzZUZvcm1hdCcsXHJcbiAgICAgICAgJ2RheWpzL3BsdWdpbi93ZWVrZGF5JyxcclxuICAgICAgICAnZGF5anMvcGx1Z2luL2xvY2FsZURhdGEnLFxyXG4gICAgICAgICdkYXlqcy9wbHVnaW4vd2Vla09mWWVhcicsXHJcbiAgICAgICAgJ2RheWpzL3BsdWdpbi93ZWVrWWVhcicsXHJcbiAgICAgICAgJ2RheWpzL3BsdWdpbi9xdWFydGVyT2ZZZWFyJyxcclxuICAgICAgICAnbG9kYXNoJyxcclxuICAgICAgICAnbnByb2dyZXNzJyxcclxuICAgICAgICAnanMtY29va2llJyxcclxuICAgICAgICAncXMnXHJcbiAgICAgIF0sXHJcbiAgICAgIGV4Y2x1ZGU6IFtcclxuICAgICAgICAnYW50LWRlc2lnbi12dWUnLFxyXG4gICAgICAgICdAYW50LWRlc2lnbi9pY29ucy12dWUnLFxyXG4gICAgICAgICdAd2FuZ2VkaXRvci9lZGl0b3InLFxyXG4gICAgICAgICdAd2FuZ2VkaXRvci9lZGl0b3ItZm9yLXZ1ZSdcclxuICAgICAgXVxyXG4gICAgfSxcclxuICAgIHNlcnZlcjoge1xyXG4gICAgICBob3N0OiAnMC4wLjAuMCcsXHJcbiAgICAgIC8vIFdpbmRvd3MgXHU0RTBBIG9wZW46dHJ1ZSBcdTk5OTZcdTZCMjFcdTYyQzlcdTRGOURcdThENTZcdTY1RjZcdTUwNzZcdTUzRDEgT09NL1x1NUQyOVx1NkU4M1x1RkYwQ1x1NjUzOVx1NEUzQVx1NjI0Qlx1NTJBOFx1NjI1M1x1NUYwMFx1NkQ0Rlx1ODlDOFx1NTY2OFxyXG4gICAgICBvcGVuOiBmYWxzZSxcclxuICAgICAgcHJveHk6IHtcclxuICAgICAgICAnL2FwaSc6IHtcclxuICAgICAgICAgIHRhcmdldDogJ2h0dHA6Ly9sb2NhbGhvc3Q6ODA4MCcsXHJcbiAgICAgICAgICB3czogZmFsc2UsXHJcbiAgICAgICAgICBjaGFuZ2VPcmlnaW46IHRydWVcclxuICAgICAgICB9XHJcbiAgICAgIH1cclxuICAgIH1cclxuICB9XHJcbn0pXHJcbiJdLAogICJtYXBwaW5ncyI6ICI7QUFBd1EsU0FBUyxjQUFjLGVBQWU7QUFDOVMsT0FBTyxTQUFTO0FBQ2hCLE9BQU8sVUFBVTtBQUNqQixPQUFPLGdCQUFnQjtBQUh2QixJQUFNLG1DQUFtQztBQUt6QyxJQUFNLG1CQUFtQixDQUFDLGNBQ3hCLGNBQWMsU0FDVixLQUFLLFFBQVEsa0NBQVcsV0FBVyxJQUNuQyxLQUFLLFFBQVEsa0NBQVcsWUFBWTtBQUUxQyxJQUFNLGdCQUFnQixDQUFDLFdBQVcsY0FBYztBQUM5QyxNQUFJLGNBQWMsUUFBUTtBQUN4QixXQUFPLGFBQWEsYUFBYSxTQUFTO0FBQUEsRUFDNUM7QUFDQSxTQUFPO0FBQ1Q7QUFFQSxJQUFNLHVCQUF1QixDQUFDLGVBQWU7QUFBQSxFQUMzQyxNQUFNO0FBQUEsRUFDTixnQkFBaUIsUUFBUTtBQUN2QixXQUFPLFlBQVksSUFBSSxDQUFDLEtBQUssTUFBTSxTQUFTO0FBcEJoRDtBQXFCTSxZQUFNLFFBQU0sU0FBSSxRQUFKLG1CQUFTLE1BQU0sS0FBSyxPQUFNO0FBQ3RDLFVBQUksY0FBYyxXQUFXLFFBQVEsT0FBTyxRQUFRLGdCQUFnQjtBQUNsRSxZQUFJLE1BQU07QUFBQSxNQUNaO0FBRUEsVUFBSSxjQUFjLFdBQVcsUUFBUSxLQUFLO0FBQ3hDLFlBQUksTUFBTTtBQUFBLE1BQ1o7QUFDQSxXQUFLO0FBQUEsSUFDUCxDQUFDO0FBQUEsRUFDSDtBQUNGO0FBR0EsSUFBTyxzQkFBUSxhQUFhLENBQUMsRUFBRSxNQUFNLFFBQVEsTUFBTTtBQUNqRCxRQUFNLE1BQU0sUUFBUSxNQUFNLFFBQVEsSUFBSSxHQUFHLEVBQUU7QUFDM0MsUUFBTSxZQUFZLElBQUksbUJBQW1CO0FBQ3pDLFFBQU0sWUFBWSxJQUFJLG1CQUFtQjtBQUN6QyxRQUFNLFlBQVksaUJBQWlCLFNBQVM7QUFDNUMsUUFBTSxRQUFRLFlBQVk7QUFFMUIsU0FBTztBQUFBLElBQ0wsU0FBUztBQUFBLE1BQ1AsSUFBSTtBQUFBLE1BQ0osU0FBUyxxQkFBcUIsU0FBUztBQUFBLE1BQ3ZDLFdBQVc7QUFBQSxRQUNULFNBQVMsQ0FBQyxVQUFVLGNBQWMsT0FBTztBQUFBLFFBQ3pDLFNBQVMsQ0FBQyxPQUFPLGNBQWMsT0FBTztBQUFBLFFBQ3RDLFVBQVU7QUFBQSxVQUNSLFNBQVM7QUFBQSxVQUNULFVBQVU7QUFBQSxVQUNWLGtCQUFrQjtBQUFBLFFBQ3BCO0FBQUEsUUFDQSxLQUFLO0FBQUEsUUFDTCxNQUFNLENBQUMsZUFBZSxjQUFjO0FBQUEsTUFDdEMsQ0FBQztBQUFBLElBQ0gsRUFBRSxPQUFPLE9BQU87QUFBQSxJQUNoQixTQUFTO0FBQUEsTUFDUCxPQUFPO0FBQUEsUUFDTCxLQUFLLEtBQUssUUFBUSxrQ0FBVyxPQUFPO0FBQUEsTUFDdEM7QUFBQSxJQUNGO0FBQUEsSUFDQSxRQUFRO0FBQUEsTUFDTixlQUFlO0FBQUEsSUFDakI7QUFBQSxJQUNBLE9BQU87QUFBQSxNQUNMLFdBQVcsWUFBWSxVQUFVLFFBQVE7QUFBQSxNQUN6QyxRQUFRLGNBQWMsV0FBVyxTQUFTO0FBQUEsTUFDMUMsYUFBYTtBQUFBLE1BQ2IsZUFBZTtBQUFBLFFBQ2IsT0FBTztBQUFBLFFBQ1AsUUFBUTtBQUFBLFVBQ04sZ0JBQWdCO0FBQUEsVUFDaEIsZ0JBQWdCO0FBQUEsVUFDaEIsZ0JBQWdCO0FBQUEsUUFDbEI7QUFBQSxNQUNGO0FBQUEsSUFDRjtBQUFBO0FBQUE7QUFBQTtBQUFBO0FBQUEsSUFLQSxjQUFjO0FBQUEsTUFDWixTQUFTO0FBQUEsUUFDUDtBQUFBLFFBQ0E7QUFBQSxRQUNBO0FBQUEsUUFDQTtBQUFBLFFBQ0E7QUFBQSxRQUNBO0FBQUEsUUFDQTtBQUFBLFFBQ0E7QUFBQSxRQUNBO0FBQUEsUUFDQTtBQUFBLFFBQ0E7QUFBQSxRQUNBO0FBQUEsUUFDQTtBQUFBLFFBQ0E7QUFBQSxRQUNBO0FBQUEsUUFDQTtBQUFBLFFBQ0E7QUFBQSxNQUNGO0FBQUEsTUFDQSxTQUFTO0FBQUEsUUFDUDtBQUFBLFFBQ0E7QUFBQSxRQUNBO0FBQUEsUUFDQTtBQUFBLE1BQ0Y7QUFBQSxJQUNGO0FBQUEsSUFDQSxRQUFRO0FBQUEsTUFDTixNQUFNO0FBQUE7QUFBQSxNQUVOLE1BQU07QUFBQSxNQUNOLE9BQU87QUFBQSxRQUNMLFFBQVE7QUFBQSxVQUNOLFFBQVE7QUFBQSxVQUNSLElBQUk7QUFBQSxVQUNKLGNBQWM7QUFBQSxRQUNoQjtBQUFBLE1BQ0Y7QUFBQSxJQUNGO0FBQUEsRUFDRjtBQUNGLENBQUM7IiwKICAibmFtZXMiOiBbXQp9Cg==
