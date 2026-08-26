# 无头 Docs（Headless Docs）设计

**日期：** 2026-08-26  
**状态：** 已实现 v1  
**范围：** docs 无壳嵌入、DocsViewer 组件、iframe 通信

---

## 1. 目标

- docs 内容与壳分离，可嵌入任意官网（iframe / 同域挂载）
- admin 不变，继续完整运营控制台
- 同一套 `Catalogue + Preview` 只读能力，宿主站自管 Header/Nav

## 2. 架构

```text
docs 入口
├── DocsLayout（可选顶栏，embed 时隐藏）
└── DocsHome → DocsViewer（核心，无顶栏）
         ├── Catalogue（readonly）
         └── Preview（readonly）

shared/components/DocsViewer  ← 可被 docs 入口或未来宿主 Vue 项目直接 import
docs/utils/embed.js           ← embed 检测 + postMessage 桥
```

## 3. 运行模式

| 模式 | 触发条件 | 表现 |
|------|----------|------|
| **整站** | 默认 `build:docs:*` | 顶栏 + 侧栏 + 正文，100vh |
| **无头 embed** | `VITE_DOCS_EMBED=true` / `?embed=1` / 在 iframe 内 | 无顶栏，填满 iframe |
| **自适应高度** | 上述 + `?embed=auto` / `VITE_DOCS_EMBED_AUTO=true` | 向父页 postMessage 高度 |

## 4. 构建产物

| 命令 | 输出 |
|------|------|
| `build:docs:shopchup` | `dist/docs-shopchup` |
| `build:docs:embed:shopchup` | `dist/docs-embed-shopchup` |
| `build:docs:embed-auto:shopchup` | `dist/docs-embed-auto-shopchup` |

Space 仍由 `VITE_DOCS_SPACE` 构建锁定。

## 5. 宿主页 iframe 集成

### 5.1 固定高度（推荐：父页控制 100vh）

```html
<iframe
  src="https://docs.example.com/docs.html?embed=1"
  style="width:100%;height:calc(100vh - 64px);border:0"
  title="文档"
></iframe>
```

### 5.2 自适应高度

```html
<iframe id="cms-docs" src="https://docs.example.com/docs.html?embed=auto" style="width:100%;border:0"></iframe>
<script>
  window.addEventListener('message', (e) => {
    if (!e.data || e.data.source !== 'cms-docs') return
    if (e.data.type === 'height') {
      document.getElementById('cms-docs').style.height = e.data.height + 'px'
    }
    if (e.data.type === 'route') {
      // 可选：同步父页 URL hash
      // history.replaceState(null, '', '/docs' + e.data.path)
    }
  })
</script>
```

### 5.3 父页驱动导航

```js
iframe.contentWindow.postMessage({
  source: 'cms-docs',
  type: 'navigate',
  path: '/articles/123'
}, '*')
```

## 6. postMessage 协议

所有消息带 `source: 'cms-docs'`。

| type | 方向 | 字段 |
|------|------|------|
| `ready` | 子 → 父 | - |
| `height` | 子 → 父 | `height: number` |
| `route` | 子 → 父 | `path`, `nodeId` |
| `navigate` | 父 → 子 | `path` 或 `nodeId` |

## 7. 非目标（v1 不做）

- Web Component 封装
- 运行时切换 Space（仍构建锁定）
- SSR / SEO（宿主负责）

## 8. 后续可选

- 宿主 Nuxt 插件封装 iframe + 协议
- `DocsViewer` 支持 props 传入 spaceSlug（单包多 space 运行时）
- 样式 shadow DOM 隔离
