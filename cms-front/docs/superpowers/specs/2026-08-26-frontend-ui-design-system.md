# CMS 前端 UI 设计系统与布局规范

**日期：** 2026-08-26  
**状态：** 待评审  
**范围：** `cms-front` 双入口（Console / Website）的视觉语言、布局结构、组件规范、深浅色模式；不含后端改动、不换编辑器、不产出 Figma 源文件（本文档为设计与实现依据，Figma 高保真可作为后续交付物对照本规范绘制）

**关联：**
- `2026-08-18-frontend-dual-entry-yuque-design.md`（双入口架构）
- `2026-08-15-cms-architecture-design.md`（整体架构）
- 现有样式基础：`src/shared/styles/tokens.less`、`yuque-article.less`

---

## 1. 设计定位

### 1.1 气质关键词

| 维度 | 目标 | 拒绝 |
|------|------|------|
| 视觉权重 | 低、克制、留白 | 厚重卡片、大色块、强对比装饰 |
| 分层方式 | 细分割线 + 留白 | 多层阴影、玻璃拟态、浓烈渐变 |
| 圆角 | 柔和 6–10px | 超大圆角、拟物凸起 |
| 动效 | hover 微反馈 | 放大抖动、弹跳、夸张过渡 |
| 信息密度 | 排版优先、长文友好 | 仪表盘化、装饰性图表堆砌 |

### 1.2 参考来源（仅借鉴布局逻辑，不 1:1 抄袭）

- **Notion Workspace**：左侧可折叠树状导航 + 顶部极简全局栏 + 主工作区；内容区居中、阅读宽度可控；模块间靠分割线与留白分层。
- **语雀工作台 / 文档后台**：双栏（目录树 + 编辑/列表）；清爽列表与表单；编辑器区大宽度、工具栏干净。
- **Notion Publish / 语雀公开知识库（Website）**：对外站点以内容为王，导航极简，阅读体验优先。

### 1.3 与现有工程的关系

当前已实现双 Vite 入口（admin / docs）、共享组件（Catalogue、Preview、RichText 等）及初步 token。本规范在**不推翻架构**的前提下，统一 Console 与 Website 的设计语言，并指导后续样式重构与 Figma 高保真产出。

---

## 2. 设计 Token（Design Tokens）

### 2.1 色彩 — 浅色模式（默认）

| Token | 值 | 用途 |
|-------|-----|------|
| `--color-primary` | `#165DFF` | 主操作、链接、选中态（海外 SaaS 柔和蓝，语雀同源色调） |
| `--color-primary-hover` | `#4080FF` | 主色 hover |
| `--color-primary-active` | `#0E42D2` | 主色按下 |
| `--color-primary-subtle` | `rgba(22, 93, 255, 0.08)` | 选中背景、轻量高亮 |
| `--color-success` | `#00B42A` | 成功、已发布（低饱和） |
| `--color-warning` | `#FF7D00` | 警告、草稿待处理 |
| `--color-danger` | `#F53F3F` | 危险、删除、错误 |
| `--color-text-primary` | `#1D2129` | 标题、正文主色 |
| `--color-text-secondary` | `#86909C` | 次要说明、占位 |
| `--color-text-tertiary` | `#C9CDD4` | 禁用、弱提示 |
| `--color-border` | `#E5E6EB` | 分割线、输入框边框 |
| `--color-border-subtle` | `#F2F3F5` | 极浅分割、hover 背景 |
| `--color-bg-page` | `#FAFBFC` | 页面底色 |
| `--color-bg-surface` | `#FFFFFF` | 卡片/面板表面（尽量少用「卡片感」） |
| `--color-bg-elevated` | `#FFFFFF` | 弹窗、下拉（配极淡阴影） |

**原则：** 中性色以灰白、浅灰分割为主；主色与状态色仅作点缀，不大面积铺色。

### 2.2 色彩 — 深色模式

| Token | 值 | 用途 |
|-------|-----|------|
| `--color-primary` | `#4080FF` | 深色下略提亮主色 |
| `--color-primary-subtle` | `rgba(64, 128, 255, 0.12)` | 选中背景 |
| `--color-success` | `#23C343` | |
| `--color-warning` | `#FF9A2E` | |
| `--color-danger` | `#F76560` | |
| `--color-text-primary` | `#F2F3F5` | |
| `--color-text-secondary` | `#A9AEB8` | |
| `--color-text-tertiary` | `#6B7785` | |
| `--color-border` | `#2E3238` | |
| `--color-border-subtle` | `#23272E` | |
| `--color-bg-page` | `#17171A` | |
| `--color-bg-surface` | `#1F1F23` | |
| `--color-bg-elevated` | `#2A2A2E` | |

**切换方式（实现期）：** `html[data-theme="light|dark"]` + CSS 变量；Console 顶栏提供主题切换；Website 默认浅色，可选跟随系统 `prefers-color-scheme`。

### 2.3 字体与排版

**字体栈（无衬线现代西文 + 中文混排）：**

```text
--font-family-base:
  "Inter", "SF Pro Text", -apple-system, BlinkMacSystemFont,
  "Segoe UI", "PingFang SC", "Hiragino Sans GB",
  "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;

--font-family-mono:
  "SF Mono", "JetBrains Mono", "Fira Code", Consolas, monospace;
```

**字号层级（PC）：**

| Token | 大小 / 行高 | 用途 |
|-------|-------------|------|
| `--text-display` | 28px / 36px, weight 600 | 页面主标题（少用） |
| `--text-title-lg` | 20px / 28px, weight 600 | 模块标题、文章 H1 对应 |
| `--text-title-md` | 16px / 24px, weight 600 | 侧栏分组、表格列头 |
| `--text-body` | 14px / 22px, weight 400 | 默认正文、表单、列表 |
| `--text-body-lg` | 15px / 26px, weight 400 | **长文阅读**（Website 详情、编辑器预览） |
| `--text-caption` | 12px / 20px, weight 400 | 辅助说明、时间戳 |
| `--text-label` | 13px / 20px, weight 500 | 表单标签、树节点 |

**长文规则（Website + 编辑器预览共用 `yuque-article` 扩展）：**
- 段落 `--text-body-lg`，行高 ≥ 1.7
- 段间距 0.75em–1em；标题上下留白大于段间距
- 链接色 `--color-primary`，无下划线，hover 可加细下划线
- 引用块：左侧 2px 主色线 + 浅灰背景，无阴影
- 代码块：等宽字体 + `#F7F8FA` 背景（深色 `#23272E`），圆角 8px

### 2.4 间距、圆角、阴影

**间距（8px 基准）：** 4 / 8 / 12 / 16 / 24 / 32 / 48 / 64

| Token | 值 |
|-------|-----|
| `--radius-sm` | 6px（小控件：Tag、Badge、小按钮） |
| `--radius-md` | 8px（输入框、按钮、列表项） |
| `--radius-lg` | 10px（卡片、弹窗、图片） |
| `--shadow-popup` | `0 4px 24px rgba(0, 0, 0, 0.06)`（浅色弹窗唯一推荐阴影） |
| `--shadow-popup-dark` | `0 4px 24px rgba(0, 0, 0, 0.32)` |
| `--shadow-none` | `none`（默认模块） |

**分层原则：** 90% 模块用 `--color-border` 1px 分割线 + padding 留白；仅 Modal / Dropdown / Drawer 遮罩层使用 `--shadow-popup`。

### 2.5 布局尺寸

| Token | Console | Website |
|-------|---------|---------|
| `--header-height` | 48px（极简顶栏） | 56px（含 logo + 少量导航） |
| `--sidebar-width` | 240px（展开）/ 56px（折叠） | 260px（可选目录侧栏） |
| `--content-max-width` | 编辑区 960px；列表区可全宽 | 阅读区 **720–780px** 居中 |
| `--workspace-gap` | 16px | 24px |

---

## 3. Console — 后台管理控制台

**用户：** 编辑、管理员  
**场景：** 文档编辑、素材库、栏目管理、权限、版本历史、预览跳转  
**布局参考：** 语雀工作台 + Notion 侧栏逻辑

### 3.1 信息架构（IA）

```text
Console
├── 全局顶栏（Logo / Space 切换 / 搜索 / 主题 / 用户）
├── 左侧导航（可折叠）
│   ├── 空间管理
│   ├── [当前 Space] 工作台
│   │   ├── 文档树（Catalogue）
│   │   └── （未来）素材库 / 设置
│   └── 底部：折叠按钮
└── 主工作区
    ├── 双栏模式 A：目录树 | 编辑/预览（SpaceWorkspace）
    ├── 单栏模式 B：空间列表 / 表格管理（SpaceManage）
    └── 全屏模式 C：沉浸式编辑（可选，隐藏侧栏）
```

### 3.2 布局线框（ASCII）

**Space 工作台（双栏 — 核心编辑场景）：**

```text
┌──────────────────────────────────────────────────────────────────┐
│  CMS · Shopchup          [空间▼]  [搜索…]     [预览] [主题] [头像] │  ← 48px 顶栏
├──────────┬───────────────────────────────────────────────────────┤
│          │  文章标题                                    [发布▼]   │
│  文档树   │  ─────────────────────────────────────────────────  │
│  ▾ 分组1  │                                                     │
│    文档A  │           编辑 / 预览区（max-width 960px 居中）        │
│    文档B  │           工具栏：干净、单行、icon+tooltip             │
│  ▾ 分组2  │                                                     │
│          │                                                     │
│  [+新建]  │  ─────────────────────────────────────────────────  │
│          │  可选：右侧大纲 Anchor（窄栏 200px，可折叠）            │
├──────────┴───────────────────────────────────────────────────────┤
│  ← 折叠                                    状态：已保存 · 14:32   │  ← 可选底栏
└──────────────────────────────────────────────────────────────────┘
```

**空间管理（单栏列表）：**

```text
┌──────────────────────────────────────────────────────────────────┐
│  CMS Admin                    [空间管理] [Shopchup] [IoT]   [头像] │
├──────────────────────────────────────────────────────────────────┤
│  空间管理                                    [筛选▼] [+ 新建空间]  │
│  ─────────────────────────────────────────────────────────────── │
│  名称          Slug        状态      排序      操作               │
│  ─────────────────────────────────────────────────────────────── │
│  Shopchup      shopchup    启用      1        进入 · 编辑         │
│  物联网         iot         启用      2        进入 · 编辑         │
└──────────────────────────────────────────────────────────────────┘
```

### 3.3 顶栏规范

- 高度 48px，底部分割线 1px `--color-border`，无阴影
- 左：Logo + 产品名（14px medium）
- 中：Space 快捷切换（文字链，active 主色 + 无背景块）
- 右：搜索（可选二期）、预览跳转、深浅色切换、用户菜单
- 禁止：渐变顶栏、厚重 shadow、多行 tab 堆叠

### 3.4 左侧导航 / 文档树（Catalogue）

- 宽度 240px，折叠后 56px（仅图标 + tooltip）
- 树节点：13px label，行高 36px，indent 16px 一级
- 选中：背景 `--color-primary-subtle`，文字 `--color-primary`，无左边框粗条
- Hover：背景 `--color-border-subtle`
- 拖拽排序：ghost 态虚线框 + 极淡背景，无 scale 动画
- 分组 / 文档图标：16px 线性 icon，低饱和
- 底部固定「新建文档 / 新建分组」文字按钮，非大色块 FAB

### 3.5 组件规范 — Console

#### 卡片
- **默认不用卡片**；用全宽分割线（`border-bottom: 1px`）划分区块
- 若必须分组：圆角 10px，边框 `1px --color-border` 或无边框，**无投影**

#### 表格 / 列表
- 无斑马线
- 表头：13px medium，`--color-text-secondary`，底部分割线
- 行高：48–52px
- Hover：行背景 `#F7F8FA`（深色 `#23272E`）
- 操作列：文字链「进入 · 编辑 · 删除」，danger 仅删除用 `--color-danger`
- 空态：插画可选，以文案为主，无大按钮堆叠

#### 表单
- 输入框：高度 36px，圆角 8px，边框 1px `--color-border`
- Focus：边框 `--color-primary`，无 glow 外发光
- 标签在上，间距 label 与 input 8px，表单项间距 20px
- 抽屉：宽 480px / 640px，从右滑入，遮罩 `rgba(0,0,0,0.3)`，面板无厚阴影
- Modal：居中，宽 480–560px，圆角 10px，仅 `--shadow-popup`

#### 编辑器区域（RichText + Preview）
- 内容区 max-width **960px**，水平居中，上下 padding 32px 48px
- 工具栏：sticky top，高度 40px，底部分割线，背景与页面同色
- 工具按钮：icon 18px，hover 圆形浅底，无下拉大面板（保持 wangEditor 能力，视觉做减法）
- 保存 / 发布：主按钮 `#165DFF`，次要按钮 ghost（边框线）
- Toast：顶部居中轻提示，圆角 8px，无 icon 动画
- 骨架屏：灰色块 `#F2F3F5`，圆角 4px， shimmer 极慢（可选）

#### 数据看板（二期 / 轻量）
- 统计数字 + 小标题，无大图标背景
- 图表：单色或主色 + 灰，无 3D、无渐变填充
- 以「内容管理」为主，不做大屏监控风

### 3.6 交互规范 — Console

| 交互 | 规范 |
|------|------|
| Hover | 背景变化 150ms ease，颜色变化 150ms |
| 点击 | 无 ripple；按钮 active 略深 5% |
| 导航树 | 展开/折叠 chevron 旋转 200ms |
| 拖拽 | 150ms 位移，opacity 0.8 |
| 版本回溯 | 侧栏或 Modal 时间线列表，选中项 primary-subtle |
| 实时预览 | 顶栏「预览」新开 tab 或右侧分屏（二期） |
| 加载 | 骨架屏优先于全屏 spinner |

---

## 4. Website — 前台展示端（docs）

**用户：** 对外访客  
**气质：** Notion Publish + 语雀公开知识库；色彩克制，内容优先

### 4.1 页面类型

| 页面 | 路由示例 | 说明 |
|------|----------|------|
| 文档首页 | `/` | Space 介绍 + 文档树入口 |
| 文档详情 | `/articles/:nodeId` | 长文阅读，目录侧栏可选 |
| 栏目列表 | `/collections/:slug`（二期） | 卡片式列表，轻分割 |
| 搜索 | `/search?q=`（二期） | 结果列表，关键词高亮 |

### 4.2 布局线框

**文档详情（阅读优先）：**

```text
┌──────────────────────────────────────────────────────────────────┐
│  [Logo]  文档中心                              [搜索]             │  ← 56px
├──────────────┬───────────────────────────────────────────────────┤
│  本页目录     │              文章标题（H1）                        │
│  · 概述       │              更新于 2026-08-26 · 阅读 5 min        │
│  · 安装       │  ─────────────────────────────────────────────    │
│  · FAQ       │                                                   │
│              │         正文区域（max-width 760px 居中）            │
│  （可折叠）   │         15px / 26px 行高，图文混排                  │
│              │                                                   │
│              │         [图片 圆角 10px]                           │
└──────────────┴───────────────────────────────────────────────────┘
```

- 小屏（<1024px）：侧栏目录收进顶部 drawer 或文内 Anchor
- 顶栏 sticky，滚动后加 1px 底部分割线（仍无 shadow）

### 4.3 组件规范 — Website

- **导航链接：** 14px，hover 主色，无下划线块
- **文档卡片（列表页）：** 非厚重卡片；左标题 + 右日期，行间分割线
- **图片：** 圆角 10px，max-width 100%，caption 12px secondary
- **代码 / 引用：** 同 2.3 长文规则
- **Hover：** 链接色变化即可；**禁止** 卡片放大、图片 scale

### 4.4 与 Console 的差异

| 维度 | Console | Website |
|------|---------|---------|
| 主色使用 | 操作按钮、选中态较多 | 仅链接、少量按钮点缀 |
| 信息密度 | 偏高（管理列表） | 低（阅读留白大） |
| 侧栏 | 可编辑树 + 管理入口 | 只读目录 / 大纲 |
| 顶栏 | Space 切换、用户 | Logo + 搜索 |
| 阅读宽度 | 960px（编辑） | 760px（阅读） |

---

## 5. 统一约束清单

### 5.1 必须遵守

- [ ] 圆角：控件 6–8px，卡片/弹窗/图片 8–10px
- [ ] 阴影：仅弹窗/下拉；其余用分割线 + 留白
- [ ] 主色 `#165DFF` 不滥用；状态色低饱和
- [ ] 深浅色两套 token 同步维护
- [ ] 长文排版 Console 预览与 Website 共用 `yuque-article` 扩展
- [ ] Ant Design Vue 主题 token 对齐本规范（ConfigProvider）

### 5.2 明确不做

- 厚重卡片、浓烈渐变、玻璃拟态、霓虹风
- 复杂数据大屏、装饰性 3D 图表
- Website 内 Space 切换器（构建时锁定 Space）
- 为视觉改版更换 wangEditor

---

## 6. Figma 高保真交付指引（PC Web）

> 本文档为设计与开发 SSOT；Figma 文件建议按下列 Frame 组织，**浅色优先，每页附带 Dark 变体**。

### 6.1 文件结构建议

```text
Figma: CMS Design System
├── 🎨 Foundations（Color / Type / Radius / Shadow / Grid）
├── 🧩 Components（Button / Input / Table / Tree / Modal / Drawer / Toast）
├── 🖥 Console
│   ├── Login
│   ├── Space Manage（列表）
│   ├── Workspace（双栏：树 + 编辑）
│   ├── Workspace（预览态）
│   └── Empty / 404 / Loading
└── 🌐 Website
    ├── Docs Home
    ├── Article Detail（含侧栏目录）
    ├── Search Results（占位）
    └── Dark variants
```

### 6.2 画板规格

- 帧宽：**1440px**（主）；备 **1280px**
- 网格：8px baseline；内容区左右安全边距 24–32px
- 组件库：Auto Layout + Variants（Button: primary/ghost/text × default/hover/disabled）

### 6.3 关键 Frame 说明

| Frame | 要点 |
|-------|------|
| Console / Workspace | 左 240px 树 + 右编辑区 960px 居中；顶栏 48px |
| Console / Space Manage | 全宽表格，无卡片容器 |
| Website / Article | 顶栏 56px + 可选左 260px 目录 + 760px 正文 |
| Dark mode | 每 Frame 复制一版，仅替换 Foundation 色板 |

---

## 7. 与现有代码映射（实现期参考，本期不写代码）

| 设计区域 | 现有文件 | 改造方向 |
|----------|----------|----------|
| Token | `shared/styles/tokens.less` | 扩展为完整 light/dark CSS 变量 |
| 长文 | `shared/styles/yuque-article.less` | 对齐 2.3 字号行高 |
| Console 壳 | `admin/layouts/AdminLayout.vue` | 顶栏 48px、侧栏折叠、分割分层 |
| 工作台 | `views/SpaceWorkspace/index.vue` | 双栏比例、编辑区宽度 |
| 文档树 | `shared/components/Catalogue/` | 树样式、拖拽反馈 |
| 编辑预览 | `Preview/`、`RichText/` | 工具栏、max-width、骨架屏 |
| 空间管理 | `admin/views/SpaceManage.vue` | 清爽表格、弱化卡片 |
| Website 壳 | `docs/layouts/DocsLayout.vue` | 阅读宽度、顶栏、侧栏 |
| 阅读页 | `docs/views/DocsHome.vue` | 详情排版、目录 |
| Ant 主题 | `admin/App.vue`、`docs/App.vue` | ConfigProvider token 映射 |

---

## 8. 分期建议

| 阶段 | 内容 | 优先级 |
|------|------|--------|
| **P0** | Token 体系（light）+ Console 布局（顶栏/双栏/树/表格）+ 长文排版 | 高 |
| **P1** | 编辑器区视觉 + 表单/Modal/Drawer 规范落地 + Website 详情页 | 高 |
| **P2** | Dark mode + 主题切换 + 骨架屏/Toast 统一 | 中 |
| **P3** | 搜索页、栏目列表、数据看板轻量统计、Figma 组件库补全 | 低 |

---

## 9. 验收标准（UI）

1. Console 首屏无厚重阴影与大色块；模块间可见 1px 分割线或 ≥24px 留白。
2. Space 工作台为双栏：左树右编辑，编辑区 max-width 960px 居中。
3. Website 文章页阅读宽度 720–780px，15px 正文、行高 ≥1.7。
4. 主色仅出现在可点击元素与选中态；状态色符合 2.1 色板。
5. 弹窗具备极淡阴影；列表/面板默认无 shadow。
6. 浅色模式完整可用；深色模式 token 齐全（P2 验收）。

---

## 10. 待确认项

1. Console 侧栏是「全局导航 + 文档树」合并，还是 Space 工作台内仅文档树？（本文档倾向：**全局顶栏切 Space，进入 Space 后左侧仅为该 Space 文档树**）
2. Website 是否需要顶栏搜索一期上线，还是 P3？
3. 深色模式 Console 是否必须 P0 同步，还是 P2 即可？

---

**文档版本：** v1.0  
**下一步：** 评审本文档 → 确认待确认项 → 再进入实现计划（不写代码直至评审通过）
