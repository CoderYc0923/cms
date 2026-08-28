# GitHub Actions CI/CD（后端 jar + 前端静态站）

**前提：** 中间件 [`MIDDLEWARE-DEPLOY.md`](./MIDDLEWARE-DEPLOY.md)、后端 [`APP-DEPLOY.md`](./APP-DEPLOY.md) 已在服务器跑通。

---

## 1. 整体架构

```text
GitHub Actions（仅手动 Run workflow）
  ├─ deploy-backend.yml   → mvn package → scp jar → systemctl restart cms-back
  └─ deploy-frontend.yml  → pnpm build → scp 静态文件 → nginx reload

服务器
  /opt/cms-back/          Spring Boot :8080（仅本机 / Nginx 反代）
  /opt/cms-front/admin/   管理端静态资源
  /opt/cms-front/docs-shopchup/   shopchup 文档站
  Nginx :80  admin    /api → 127.0.0.1:8080
  Nginx :8081 docs    /api → 127.0.0.1:8080
```

生产前端 `VUE_APP_API_BASE_URL=/`（见 `cms-front/.env.production`），浏览器请求同域 `/api`，由 Nginx 反代，**无需改后端 CORS**。

---

## 2. 服务器一次性准备

### 2.1 目录

```bash
mkdir -p /opt/cms-front/admin /opt/cms-front/docs-shopchup
mkdir -p /opt/cms-back/logs
```

### 2.2 安装 Nginx

```bash
# Alibaba Cloud Linux / CentOS
dnf install -y nginx
systemctl enable nginx
systemctl start nginx
```

### 2.3 部署 Nginx 配置

```bash
# 在仓库 clone 或 scp 后
cp /path/to/cms-back/deploy/nginx/cms-front.conf /etc/nginx/conf.d/cms-front.conf
nginx -t
systemctl reload nginx
```

### 2.4 防火墙（阿里云轻量控制台）

| 端口 | 用途 |
|------|------|
| 22 | SSH |
| 80 | Admin 管理端 |
| 8081 | Docs shopchup |
| **不要** 对公网开 8080、3307、6379 等 |

### 2.5 后端 systemd

见 [`APP-DEPLOY.md`](./APP-DEPLOY.md)，确保 `cms-back` 已 `enable` 且在跑。

### 2.6 OSS CORS（上传功能）

在阿里云 OSS 桶 CORS 中增加来源（按你实际访问地址）：

- `http://<公网IP>`
- `http://<公网IP>:8081`

方法含 `PUT`、`POST`、`GET`、`OPTIONS`；ExposeHeader 含 `ETag`。

---

## 3. GitHub Secrets（仓库 Settings → Secrets → Actions）

| Secret | 说明 |
|--------|------|
| `DEPLOY_HOST` | 服务器公网 IP |
| `DEPLOY_USER` | `root` |
| `DEPLOY_SSH_KEY` | 部署专用 SSH **私钥** 全文 |
| `DEPLOY_PORT` | 可选，默认 `22` |

### 生成部署密钥（本机 PowerShell）

```powershell
ssh-keygen -t ed25519 -C "github-deploy" -f $env:USERPROFILE\.ssh\cms_deploy
```

公钥写入服务器：

```bash
mkdir -p ~/.ssh && chmod 700 ~/.ssh
echo "公钥内容" >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

私钥内容复制到 GitHub Secret `DEPLOY_SSH_KEY`。

本机验证：

```powershell
ssh -i $env:USERPROFILE\.ssh\cms_deploy root@<公网IP>
```

---

## 4. Workflow 说明

| 文件 | 触发条件 | 行为 |
|------|----------|------|
| `.github/workflows/deploy-backend.yml` | 仅手动 Run workflow | 打包 jar → 上传 → restart |
| `.github/workflows/deploy-frontend.yml` | 仅手动 Run workflow | build admin + docs-shopchup → 上传 → reload nginx |

**push 到 main 不会自动部署**；需要发版时到 Actions 页分别点 Run。

---

## 5. 首次启用 CI 步骤

```text
1. 服务器完成 §2（Nginx、目录、防火墙、cms-back systemd）
2. GitHub 仓库配置 §3 Secrets
3. 本地 commit 并 push .github/workflows/*.yml 到 main
4. Actions 页查看两条 workflow 是否绿色
5. 浏览器访问 http://<公网IP>/ 与 http://<公网IP>:8081/
```

---

## 6. 访问地址

| 站点 | URL |
|------|-----|
| Admin 管理端 | `http://<公网IP>/` |
| Docs shopchup | `http://<公网IP>:8081/` |
| API（经 Nginx） | `http://<公网IP>/api/...` |

有域名后，把 `cms-front.conf` 里 `server_name _` 改成你的域名，并考虑 HTTPS（Certbot / 阿里云证书）。

---

## 7. 日常开发流程

```text
改代码 → push main（只更新仓库，不部署）
要发版 → Actions → Deploy Backend / Deploy Frontend → Run workflow
本地联调 → 仍可用 SSH 隧道 + pnpm dev（见 MIDDLEWARE-DEPLOY §7）
```

---

## 8. 常见问题

| 现象 | 处理 |
|------|------|
| Actions SSH 失败 | 检查 Secrets、公钥是否在服务器 `authorized_keys` |
| 后端 Restart 后 health 失败（curl exit 7） | 看 `/opt/cms-back/logs/app.log`；确认 jar 在 `/opt/cms-back/*.jar` 而非嵌套 `cms-back/cms-back-admin/target/`（Upload 需 `strip_components: 3`） |
| 前端 404 刷新丢失 | 确认 nginx `try_files` 与 root 路径正确 |
| 登录超时 | 后端未启动；或 Nginx `/api/` 未反代到 8080 |
| OSS 上传失败 | 补 OSS CORS 来源为公网 IP |
| 仅 docs 要发版 | Actions 手动 Run deploy-frontend |

---

## 9. 后续扩展

- **iot Docs**：在 `deploy-frontend.yml` 加 `pnpm build:docs:iot`，nginx 再加 `8082` server 块
- **HTTPS**：`certbot --nginx` 或阿里云证书挂到 443
- **合并一条 pipeline**：需要时用 `workflow_run` 串前后端，当前拆分更清晰
