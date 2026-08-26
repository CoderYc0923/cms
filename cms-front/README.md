# CMS Frontend

Vue 3 + Vite 双入口（admin / docs）。

## 双入口开发

```bash
pnpm install

# Admin 运营站（5173）
pnpm dev:admin

# Docs 只读站（5174 / 5175，Space 由 VITE_DOCS_SPACE 锁定）
pnpm dev:docs:iot
pnpm dev:docs:shopchup

# 无头 embed 模式（无顶栏，供 iframe 嵌入）
pnpm dev:docs:embed:shopchup      # 5177
pnpm dev:docs:embed-auto:shopchup # 5178，自适应高度
```

访问地址：

- Admin: http://localhost:5173/ （或 `/admin.html` 会自动跳转到 `/`）
- Docs (iot): http://localhost:5174/docs.html
- Docs (shopchup): http://localhost:5175/docs.html
- Docs embed (shopchup): http://localhost:5177/docs.html
- Docs embed 任意整站加 `?embed=1` 也可触发无头模式

也可在仓库根目录执行（会自动进入 `cms-front`）：

```bash
pnpm dev:admin
pnpm dev:docs:shopchup
```

## 构建

```bash
pnpm build:admin           # → dist/admin
pnpm build:docs:iot        # → dist/docs-iot
pnpm build:docs:shopchup   # → dist/docs-shopchup
pnpm build:docs:embed:shopchup   # → dist/docs-embed-shopchup（无头）
```

## 无头 Docs 嵌入

核心组件：`src/shared/components/DocsViewer/index.vue`（Catalogue + Preview，无顶栏）。

**触发无头模式（任一即可）：**

- 构建：`VITE_DOCS_EMBED=true`（见 `.env.docs.embed.*`）
- 运行时：`?embed=1` 或页面在 iframe 内
- 自适应高度：再加 `?embed=auto` 或 `VITE_DOCS_EMBED_AUTO=true`

固定高度（常用）：
```html
<iframe
  src="https://your-cms-docs/docs.html?embed=1"
  style="width:100%;height:calc(100vh - 64px);border:0"
></iframe>
```
自适应高度：
```html
<iframe id="docs" src="...?embed=auto" style="width:100%;border:0"></iframe>
<script>
window.addEventListener('message', e => {
  if (e.data?.source === 'cms-docs' && e.data.type === 'height') {
    docs.style.height = e.data.height + 'px'
  }
})
</script>
```

## 目录（双入口）

```text
cms-front/
  admin.html / docs.html
  src/
    admin/          # 运营入口（AdminLayout、JWT、可编辑）
    docs/           # 文档入口（DocsLayout、只读、VITE_DOCS_SPACE）
    shared/         # 共用 bootstrap、styles
    components/     # 逐步迁入 shared/components
    views/          # 现有页面，admin 复用
```

## 环境变量

| 文件 | 说明 |
|------|------|
| `.env.admin` | `VITE_APP_TARGET=admin` |
| `.env.docs.iot` | `VITE_APP_TARGET=docs`, `VITE_DOCS_SPACE=iot` |
| `.env.docs.shopchup` | `VITE_APP_TARGET=docs`, `VITE_DOCS_SPACE=shopchup` |
| `.env.docs.embed.shopchup` | 同上 + `VITE_DOCS_EMBED=true` |
| `.env.docs.embed-auto.shopchup` | 同上 + `VITE_DOCS_EMBED_AUTO=true` |

## 注意事项

- Node.js >= 18.12
- 首次 `pnpm install` 若提示 build scripts 被忽略，执行 `pnpm approve-builds` 并勾选 `esbuild`、`vue-demi`
- 旧单入口 `src/main.js` 暂保留，默认 `pnpm dev` 已指向 `dev:admin`
