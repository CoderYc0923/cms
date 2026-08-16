# 语雀风格 CMS 架构设计

**日期：** 2026-08-15  
**状态：** 待实现（设计已评审通过，待写入实现计划）  
**范围：** MVP 架构与前后端边界；不含 RAG/搜索/版本历史实现

## 1. 背景与目标

现有前端（Vue 3 + Vite + Ant Design Vue）已具备语雀式目录（分组/菜单/文章）、富文本、登录与同页编辑/预览切换。目标是落地完整 CMS：

1. **一套代码、两个端**：运营 `admin` 可编辑；对外 `docs` 仅公开只读。
2. **后端 Java + PostgreSQL**（不采用 MongoDB）。
3. **后期 AI Agent / 独立 RAG**：CMS 作为内容源，通过稳定 ID + 发布事件同步；本期不实现 RAG。

### 非目标（MVP 不做）

- 全文搜索、评论、文档版本历史/回滚
- 细粒度 RBAC、多租户白标
- 向量库、问答、RAG 同步 worker
- 将后端拆成多个微服务或前置 BFF/网关

## 2. 产品决策摘要

| 决策项 | 选择 |
|--------|------|
| 前端形态 | 单仓库双 Vite 入口：`admin` / `docs` |
| docs 访问 | 完全公开，无需登录 |
| 发布流程 | 草稿 / 已发布；支持下架回草稿 |
| 已发布再编辑保存 | **立刻对外生效**，并写 `updated` 事件 |
| 内容组织 | 多知识库（Space），如 Shopchup、物联网 |
| 数据库 | PostgreSQL |
| 后端形态 | 单体 Spring Boot |
| Agent/RAG | 仅预留 `publish_events` 与 public 读模型 |

## 3. 系统架构与仓库边界

```text
┌─────────────────┐     ┌─────────────────┐
│  admin 站点      │     │  docs 站点       │
│  (运营编辑)      │     │  (公开只读)      │
│  Vite: admin     │     │  Vite: docs      │
└────────┬────────┘     └────────┬────────┘
         │ /api/admin/*           │ /api/public/*
         │ (需登录)               │ (无登录, 仅已发布)
         └───────────┬───────────┘
                     ▼
            ┌─────────────────┐
            │  Spring Boot     │
            │  单体 API        │
            └────────┬────────┘
                     ▼
            ┌─────────────────┐
            │  PostgreSQL      │
            └─────────────────┘
```

### 仓库

| 位置 | 职责 |
|------|------|
| 本前端仓库 | 双入口构建；复用 Catalogue、Preview、RichText、登录等 |
| 新建后端（独立仓库或本仓 `server/`） | Java + Spring Boot + PostgreSQL |

### 边界原则

- 写操作仅 `/api/admin/**`，必须鉴权。
- 公开读仅 `/api/public/**`，只返回 `published` 文章及可见目录。
- 草稿永不出现在 public API（访问草稿/下架 → 404）。
- RAG 后期消费事件或 public 接口；不进入 CMS 主写路径。

### 相对现状的前端变化

- 同页「编辑/预览」+ `v-auth` → 拆为两个入口：admin 默认可编；docs 无编辑 UI、无登录。
- 顶部 Shopchup / 物联网 → 升级为 **Space**（`slug` 驱动路由与 API）。

## 4. 领域模型与数据表

### 实体

| 实体 | 说明 |
|------|------|
| User | 运营账号（仅 admin） |
| Space | 知识库；`slug` 用于 docs URL |
| Node | 目录树节点：`group` / `menu` / `article` |
| Article | 正文与发布态；与 `article` 类型 Node 1:1 |
| PublishEvent | 发布/下架/更新事件；MVP 只写入，不消费 |

### 与现有前端枚举对应

- `source` → `spaces.slug`
- `MENU_TYPE`（GROUP/MENU/ARTICLE）→ `nodes.type`
- 文章上线/下线 → `articles.publish_status`（`draft` \| `published`）
- 目录节点可用简单 `status` 控制是否在树中展示

### 表结构

**users**  
`id`, `username`, `password_hash`, `display_name`, `status`, `created_at`, `updated_at`

**spaces**  
`id`, `name`, `slug`（唯一）, `description`, `sort`, `status`, `created_at`, `updated_at`

**nodes**  
`id`, `space_id`, `parent_id`（可空）, `type`（`group`\|`menu`\|`article`）, `title`, `sort`, `status`, `deleted_at`, `created_at`, `updated_at`  
索引：`(space_id, parent_id, sort)`

**articles**  
`id`, `node_id`（唯一）, `space_id`, `content`（TEXT 或 JSONB）, `publish_status`（`draft`\|`published`）, `published_at`, `created_by`, `updated_by`, `deleted_at`, `created_at`, `updated_at`

**publish_events**  
`id`, `article_id`, `space_id`, `event_type`（`published`\|`unpublished`\|`updated`）, `occurred_at`, `payload`（可选摘要）

### 树与可见性

- **admin**：某 Space 整树 + 全部文章（含草稿）。
- **docs**：仅包含「文章为 published」的 article 节点及其祖先路径；草稿文章节点不出现在公开树。
- **删除（MVP）**：禁止删除仍有子节点的 group/menu；文章与节点采用软删（`deleted_at`），public/admin 列表默认过滤已删数据。

## 5. API、鉴权与数据流

### 鉴权

- admin：登录发放 Token；无效/缺失 → 401。可延续现有 RSA 传密 + 服务端验密。
- public：无登录；不得暴露写接口。
- MVP 单一运营角色，不做 RBAC。

### Admin API（需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/admin/auth/login` | 登录 |
| GET/POST/PUT | `/api/admin/spaces`… | Space CRUD |
| GET | `/api/admin/spaces/{slug}/tree` | 完整树（含草稿） |
| POST/PUT/DELETE | `/api/admin/nodes`… | 节点维护 |
| GET/PUT | `/api/admin/articles/{id}` | 读/存正文 |
| POST | `/api/admin/articles/{id}/publish` | 发布 |
| POST | `/api/admin/articles/{id}/unpublish` | 下架 |

### Public API（无登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/public/spaces` | 已启用 Space 列表 |
| GET | `/api/public/spaces/{slug}/tree` | 仅已发布可见树 |
| GET | `/api/public/spaces/{slug}/articles/{id}` | 已发布正文；否则 404 |

### 数据流

1. **保存草稿/正文**：`PUT article`；若当前已是 `published`，保存后仍为 `published`，内容立即对 docs 生效，并写入 `publish_events.updated`。
2. **发布**：`draft` → `published`，设置 `published_at`，写 `published` 事件。
3. **下架**：`published` → `draft`，写 `unpublished` 事件；public 立即不可见。
4. **docs 阅读**：Space 列表 → public tree → public article。

### 后期 RAG 预留（不实现）

- 同步依据：`publish_events` 和/或 public 文章接口。
- 稳定标识：`articles.id` / `nodes.id` 不因改标题而变。
- CMS 不内嵌向量库与问答。

## 6. 错误处理与安全

| 场景 | 行为 |
|------|------|
| admin 未登录/Token 无效 | 401，前端清登录态 |
| 资源不存在 | 404 |
| 删除非空目录等冲突 | 400 或 409 + 明确文案 |
| public 访问草稿/下架 | 404（不泄露存在性） |
| 非法 slug / 校验失败 | 404 / 400 |
| 未处理异常 | 500；docs 不返回堆栈 |

统一响应：`{ code, message, data }`（与现有前端约定对齐）。

**安全底线**

- public 无写方法。
- docs 渲染使用 XSS 消毒（沿用 `dompurify`）；后端可按需做内容策略。
- 密码哈希存储；CORS 分别配置 admin / docs 源。

## 7. 测试要点（MVP）

**后端**

- 发布/下架后 public tree 与 article 可见性。
- 已发布再保存 → public 内容更新 + `updated` 事件。
- 未登录访问 admin → 401。

**前端**

- admin 有编辑能力；docs 无编辑入口且无 Token 可读。
- Space 切换与树、正文联动。

不做：搜索、版本、RAG worker 的完整测试（仅保证事件表写入）。

## 8. 为何不用 MongoDB

MongoDB 适合半结构化、高吞吐流水、字段频繁变化的场景。本 CMS 核心是 Space、树形目录、发布态与运营鉴权，关系清晰、需要一致的树操作与查询；PostgreSQL（TEXT/JSONB 存正文）更合适。富文本的灵活性用 JSONB/TEXT 字段即可，无需整库文档模型。

## 9. 成功标准

- 运营可在 admin 维护多 Space目录与文章，草稿/发布/下架闭环可用。
- 匿名用户可在 docs 仅看到已发布内容。
- 数据模型与 `publish_events` 足以支撑后续独立 RAG 接入，而无需重构主表。
- 前后端边界清晰，可在单体内完成 MVP 上线。
