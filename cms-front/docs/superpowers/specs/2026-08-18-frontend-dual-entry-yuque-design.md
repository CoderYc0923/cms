# 前端双入口与语雀向改造设计

**日期：** 2026-08-18  
**状态：** 待实现（设计已评审，文档先落盘，暂不提交）  
**范围：** `cms-front` 双 Vite 入口、docs 按 Space 独立构建、轻量语雀布局、富文本所见即所得样式；不含换编辑器、Nuxt 仓库改动、细粒度 RBAC

**关联：** 补充并细化 `2026-08-15-cms-architecture-design.md` 中的「单仓库双 Vite 入口」；数据库已由 PostgreSQL 调整为 MySQL（以后端现状为准）。

---

## 1. 背景与目标

当前前端是**单入口 SPA**：管理与阅读混在同页（`v-auth` + 编辑/预览），顶栏可在 Shopchup / 物联网间切换。业务目标变为：

1. **admin**：独立站点访问；可切换 Space；可编辑、上下架；JWT 鉴权。
2. **docs**：只读；按 Space **各自构建并部署**到对应官网（如物联网官网、Shopchup 官网）的 `/docs`；无 Space 切换、无登录。
3. **视觉**：向语雀靠拢，但**不重度重做**；保留 Catalogue、wangEditor、核心交互。
4. **富文本**：编辑区与预览/docs 阅读共用语雀向正文样式，尽量所见即所得（R2）。

### 非目标（本期不做）

- 更换富文本编辑器或仿语雀工具栏 1:1
- iframe 嵌入 Nuxt
- docs 包内提供 Space 切换器
- 细粒度 RBAC、多租户白标
- 修改 Nuxt 官网仓库（仅约定对接方式）

---

## 2. 产品与部署决策

| 决策项 | 选择 |
|--------|------|
| 入口形态 | 双 Vite 入口（方案 A / 目录方案 1） |
| admin 访问 | 独立站点（独立域名或子域） |
| docs 访问 | 挂在各官网同域路径（如 `www.xxx.com/docs/**`）；菜单普通链接（同页或新标签），不 iframe |
| docs Space | 构建时环境变量锁定（D1）：`VITE_DOCS_SPACE=iot` / `shopchup` |
| 视觉深度 | 轻量语雀化（S3 收敛版）：Layout + tokens + 正文样式；保留现有组件 |
| 富文本 | R2：预览与 wangEditor 编辑区共用 `yuque-article` 样式 |
| admin Space | 保留顶栏切换，方便统一运营 |

### 部署示意

```text
admin.xxx.com              → build:admin
iot-site.xxx.com/docs/**   → build:docs:iot      （VITE_DOCS_SPACE=iot）
shopchup-site/docs/**      → build:docs:shopchup （VITE_DOCS_SPACE=shopchup）
```

本地开发：admin 与 docs 分端口（如 5173 / 5174）；无 Nuxt 时可直接打开 docs 端口调试。

---

## 3. 前端架构与目录

```text
cms-front/
  admin.html
  docs.html
  vite.config.js              # MPA 双 input；docs 读取 VITE_DOCS_SPACE
  package.json
    scripts:
      dev:admin / build:admin
      dev:docs:iot / build:docs:iot
      dev:docs:shopchup / build:docs:shopchup
  src/
    admin/
      main.js
      router.js
      layouts/AdminLayout.vue
      （登录、鉴权 store、admin API）
    docs/
      main.js
      router.js
      layouts/DocsLayout.vue
      （无登录；Space 来自 import.meta.env.VITE_DOCS_SPACE）
    shared/
      components/             # Catalogue、RichText、Article、Preview、Anchor…
      styles/yuque-article.less
      styles/tokens.less      # 可选：颜色/间距 token
      api/public.js           # 公开读
```

### 边界原则

- **admin 入口**：可引入登录、refresh、admin 写接口；Catalogue 可编。
- **docs 入口**：不引入登录/refresh；Catalogue 只读；请求仅 `/api/public/**`，且始终带构建期 Space。
- **shared**：与端无关的 UI 与正文样式；通过 props（如 `readonly`）区分能力，避免 docs 依赖 admin 模块。

---

## 4. 布局

### docs

- 顶栏：当前 Space 名称（展示用），无切换、无登录。
- 左：只读 Catalogue；中：正文（`yuque-article`）；右：可选 Anchor 大纲。
- 去掉 ProLayout「管理系统」观感。

### admin

- 顶栏：Space 切换（物联网 / Shopchup）+ 登录态。
- 左：可编 Catalogue（含上下架等现有能力）；中：RichText 编辑 / Preview，共用 `yuque-article`。
- 不重写 Catalogue / wangEditor 业务逻辑；Layout 与间距向语雀工作台靠拢。

### 视觉：做 / 不做

| 做 | 不做 |
|----|------|
| 共用 tokens、两端 Layout | 替换 Ant Design 组件体系 |
| 正文高度相似语雀 | 编辑器 chrome 1:1 语雀 |
| 收敛现有 `resetEditor.less` 进 `yuque-article` | iframe 嵌官网 |

---

## 5. 富文本所见即所得（R2）

1. 新增 `shared/styles/yuque-article.less`（标题、段落、列表、引用、代码块、表格、图片等）。
2. docs 阅读与 admin Preview：根节点使用 `.yuque-article`。
3. wangEditor 编辑区 content 容器挂载**同一套**样式，使编辑态与阅读态一致。
4. 工具栏外观本期可不改；以正文一致性为验收标准。

---

## 6. 鉴权与 API

| | admin | docs |
|--|--------|------|
| API 前缀 | `/api/admin/**` | `/api/public/**` |
| 登录 | 需要 | 不需要 |
| Token | access + refresh；`Authorization: Bearer`；401 单飞 refresh | 不携带 |
| Space | 运行时切换 | `VITE_DOCS_SPACE` 构建锁定 |

相对现状的改造：

- 废弃 RSA 公钥登录与 Cookie `BACK_USERID`，改为双 Token（仅 admin）。
- 取消「同页 v-auth 决定能否编辑」作为主模型；改为两端分离。
- CORS：覆盖 admin 独立域与各官网 docs 域（本地继续允许 localhost 端口 pattern）。

---

## 7. 落地顺序

1. 双入口脚手架（HTML、Vite MPA、npm scripts、`VITE_DOCS_SPACE`）。
2. 组件迁入 `shared` + AdminLayout / DocsLayout；docs 锁 Space、Catalogue 只读。
3. admin 对接 JWT login / refresh / logout + axios 拦截器。
4. `yuque-article`：预览与编辑区共用（R2 关键）。
5. 顶栏/间距等轻量语雀抛光。
6. 约定 `build:admin`、`build:docs:iot`、`build:docs:shopchup` 产物与部署路径。

---

## 8. 验收要点

- `dev:admin` 可切换 Space，登录后可编、可上下架。
- `dev:docs:iot` 仅物联网内容，无切换器、无登录；`shopchup` 同理。
- 同一篇文章在编辑区与 docs/预览中正文观感一致（语雀向）。
- docs 构建产物不依赖刷新 Token 流程即可运行。

---

## 9. 风险与备注

- Catalogue 体积大、编辑/只读耦合：拆 props/`readonly` 时避免一次大改行为回归。
- wangEditor 样式穿透需确认 content 容器 class 挂载点，避免只影响外壳。
- 双入口初期迁移成本高于「只加 env」；换来的是 docs 按官网独立部署的清晰边界（已选 D1）。
