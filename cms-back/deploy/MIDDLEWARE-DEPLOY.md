# CMS 中间件单机部署教程（Linux 命令行 / 阿里云轻量 2G）

**目标：** 用 Docker **拉取官方镜像** 并在服务器上启动 MySQL、Redis、RocketMQ、XXL-Job；**不需要**上传 Java 源码或整个 `cms-back` 工程。

**部署包路径（仓库内）：** `cms-back/deploy/middleware/`  
**服务器推荐目录：** `/opt/cms-middleware`

**范围：** 仅中间件；Spring Boot jar、前端 Nginx 下一步再做。

---

## 0. 先搞清：镜像 vs 文件

| 你做的 | 说明 |
|--------|------|
| `docker compose up` | Docker 从仓库 **自动 pull** `mysql:8.0`、`redis:7.2` 等 **镜像** |
| 上传到服务器 | 只需 **compose + .env + 少量配置**（几 KB～几十 KB），不是镜像本身 |
| **不需要** | `cms-back` 的 `src/`、`pom.xml`、前端、`target/` |

独立部署包结构：

```text
cms-back/deploy/middleware/          →  复制到服务器 /opt/cms-middleware/
├── docker-compose.yml               # 起哪些容器
├── .env.example  → 复制为 .env      # 密码、端口、JVM 堆
├── rocketmq/
│   ├── broker-local.conf            # Broker 挂载配置
│   └── broker-prod.conf             # 可选
├── scripts/
│   └── init-xxl-job-db.sh           # 一次性初始化 xxl_job 库
└── README.md
```

> **本地开发**仍可用仓库根目录 `cms-back/docker-compose.yml`（与 middleware 包等价，路径不同）。**服务器只传 `deploy/middleware` 即可。**

---

## 1. 架构一览

```text
┌─────────────────────────────────────────────────────────┐
│  阿里云轻量 2核2G + Linux 命令行                          │
│  工作目录：/opt/cms-middleware                            │
│                                                         │
│  docker compose                                         │
│    ├─ cms-mysql          :3307 → 3306                   │
│    ├─ cms-redis          :6379                          │
│    ├─ cms-rocketmq-namesrv :9876                        │
│    ├─ cms-rocketmq-broker  :10911/10912/10909           │
│    └─ cms-xxl-job-admin    :8088                        │
│                                                         │
│  （下一步）cms-back-admin.jar  → 8080                     │
│  （下一步）Nginx → cms-front/dist                         │
└─────────────────────────────────────────────────────────┘
```

应用侧连接（写入 **本机或服务器** 的 `cms-back/.env`，非 middleware 目录）：

| 变量 | 同机典型值 |
|------|------------|
| `DB_HOST` / `DB_PORT` | `127.0.0.1` / `3307` |
| `REDIS_HOST` / `REDIS_PORT` | `127.0.0.1` / `6379` |
| `ROCKETMQ_NAME_SERVER` | `127.0.0.1:9876` |
| `XXL_JOB_ADMIN_ADDRESSES` | `http://127.0.0.1:8088/xxl-job-admin` |

---

## 2. 服务器准备

### 2.1 登录

```bash
ssh root@<你的公网IP>
```

### 2.2 安装 Docker（命令行）

**Alibaba Cloud Linux 3：** 不要 `dnf install docker`（会与 podman-docker 冲突）。用 Docker CE：

```bash
sudo dnf -y install dnf-plugins-core
sudo dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo dnf remove -y podman-docker 2>/dev/null || true
sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo systemctl enable --now docker
docker --version
docker compose version
```

**Ubuntu：**

```bash
sudo apt -y install docker.io docker-compose-v2
sudo systemctl enable --now docker
```

### 2.3 内存与 Swap（2G 建议）

```bash
free -h
# Swap 不足时可再加 1～2G，见前文 swap 命令
```

`.env` 里 RocketMQ 务必：

```bash
ROCKETMQ_NAMESRV_JAVA_OPT=-Xms128m -Xmx256m
ROCKETMQ_BROKER_JAVA_OPT=-Xms128m -Xmx256m
```

### 2.4 防火墙

- **firewalld 为 inactive**：系统层不拦端口，主要靠 **阿里云轻量「防火墙」** 控制台。
- **同机部署演示**：控制台 **不要** 对公网放行 3307、6379、9876、10911、8088。
- **本地 Windows 连远程中间件**：临时放行上述端口并限源 IP，或改用 **SSH 隧道**（更安全）。

---

## 3. 上传中间件部署包（不是整仓）

**原则：** 只要服务器上最终有 `/opt/cms-middleware/` 目录里的文件即可；**不强制**在本机执行 `scp`。你已在本地打好 `middleware.tgz` 时，任选下面一种方式把包弄到服务器 `/tmp/`（或任意目录），再解压。

### 3.1 方式 A — 本机 scp / sftp（常见）

**直接传文件夹：**

```powershell
scp -r D:\code-self\cms\cms-back\deploy\middleware root@<服务器IP>:/opt/cms-middleware
```

**或传已打好的包（你正在用的方式）：**

```powershell
scp middleware.tgz root@<服务器IP>:/tmp/
```

服务器解压：

```bash
sudo mkdir -p /opt
sudo tar -xzf /tmp/middleware.tgz -C /opt
sudo mv /opt/middleware /opt/cms-middleware
```

### 3.2 方式 B — 图形工具上传（不必命令行 scp）

已打包 `middleware.tgz` 时，可用 **WinSCP**、**FileZilla**、**MobaXterm** 等：

1. 协议选 **SFTP**，主机 `<服务器IP>`，用户 `root`，密码或密钥登录  
2. 把 `middleware.tgz` 拖到服务器 `/tmp/`  
3. SSH 进服务器执行上面「解压」三条命令  

阿里云 **Workbench 远程连接** 若带「上传文件」按钮，也可直接选 `middleware.tgz` 上传到 `/tmp/`，再 SSH 解压（以控制台实际功能为准）。

### 3.3 方式 C — 服务器上 git clone（不经本机上传）

代码已在 **GitHub / Gitee / 阿里云 Code** 等平台时，在服务器执行：

```bash
git clone <仓库地址> /tmp/cms-repo
sudo cp -a /tmp/cms-repo/cms-back/deploy/middleware /opt/cms-middleware
# 或 clone 后只拷贝 deploy/middleware 目录
```

适合服务器能访问 git、且仓库里已提交 `deploy/middleware` 的改动（含你改好的 `broker-local.conf`）。

### 3.4 方式 D — 先传到网盘/OSS，服务器 wget 下载

1. 把 `middleware.tgz` 上传到 **OSS 临时目录**、GitHub Release、内网文件站等，得到 **HTTPS 下载链接**  
2. 服务器上：

```bash
cd /tmp
curl -fLO '<下载链接>' -o middleware.tgz
# 或 wget '<下载链接>' -O middleware.tgz
sudo mkdir -p /opt
sudo tar -xzf middleware.tgz -C /opt
sudo mv /opt/middleware /opt/cms-middleware
```

注意链接不要长期公开；OSS 可设短期签名 URL。

### 3.5 方式 E — 整仓 clone（可选，非必须）

仅当你希望服务器上也有完整后端源码时再整仓 `git clone`；**只跑中间件不必**。

### 3.6 配置 `.env`

```bash
cd /opt/cms-middleware
cp .env.example .env
chmod 600 .env
vim .env
```

必改：`MYSQL_PASSWORD`、`MYSQL_ROOT_PASSWORD`。

检查 3306 是否被占用：

```bash
ss -tlnp | grep 3306
# 有输出则保持 MYSQL_PORT=3307
```

### 3.7 RocketMQ Broker 地址

默认 `rocketmq/broker-local.conf` 中 `brokerIP1=127.0.0.1`，适合 **应用与 compose 同机**。

- 远程客户端连公网：改为服务器 **公网 IP**，然后 `docker compose restart rocketmq-broker`。
- 内网多机：用 `broker-prod.conf` 并改 compose 挂载路径。

---

## 4. 启动中间件

```bash
cd /opt/cms-middleware

docker compose up -d db redis
docker compose ps
docker logs cms-mysql --tail 20

docker compose up -d rocketmq-namesrv rocketmq-init rocketmq-broker

chmod +x scripts/init-xxl-job-db.sh
./scripts/init-xxl-job-db.sh

docker compose up -d xxl-job-admin

# 或一次性：
docker compose up -d
docker compose ps
```

除一次性容器 `cms-rocketmq-init` 外，其余应为 `running` / `healthy`。

---

## 5. 验收

### MySQL

```bash
cd /opt/cms-middleware
set -a && source .env && set +a
docker exec -it cms-mysql mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SHOW DATABASES;"
```

### Redis

```bash
docker exec -it cms-redis redis-cli ping
```

### RocketMQ

```bash
docker logs cms-rocketmq-broker --tail 30
```

### XXL-Job

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8088/xxl-job-admin/
```

浏览器（需安全组放行 8088）：`http://<公网IP>:8088/xxl-job-admin`  
默认 `admin` / `123456`，登录后改密码。

---

## 6. 让 CMS 应用连上已部署的中间件

middleware 目录的 `.env` **只给 compose 用**。应用在 **`cms-back/.env`**（开发机或同机 jar 目录）里配置：

```bash
DB_HOST=127.0.0.1          # 同机；远程则填服务器 IP
DB_PORT=3307                 # 与 middleware/.env 的 MYSQL_PORT 一致
DB_NAME=cms
DB_USERNAME=cms
DB_PASSWORD=与 middleware 里 MYSQL_PASSWORD 一致

REDIS_HOST=127.0.0.1
REDIS_PORT=6379

ROCKETMQ_NAME_SERVER=127.0.0.1:9876
XXL_JOB_ADMIN_ADDRESSES=http://127.0.0.1:8088/xxl-job-admin
```

**本地 Windows 开发、中间件在云上：** 把 host 改为公网 IP，安全组临时放行或 SSH 隧道；RocketMQ 改 `brokerIP1`。

**同机启动 jar（下一步）：**

```bash
cd /path/to/cms-back
set -a && source .env && set +a
java -Xms256m -Xmx512m -jar cms-back-admin/target/cms-back-admin-*.jar --spring.profiles.active=local
```

---

## 7. 运维速查

```bash
cd /opt/cms-middleware
docker compose ps
docker compose stop
docker compose up -d
docker compose logs -f --tail 100 rocketmq-broker
docker volume ls | grep cms-middleware
```

数据卷名前缀为 `cms-middleware-*`（与本地 `cms-back/docker-compose.yml` 的 `cms-back-*` 相互独立，勿混用同一目录反复 compose）。

---

## 8. 常见问题

| 现象 | 处理 |
|------|------|
| OOM / 卡死 | 减小 RocketMQ 堆、加 Swap、`docker stats` |
| `dnf install docker` 冲突 | 用 Docker CE，见 §2.2 |
| RocketMQ 连不上 | 检查 `brokerIP1`、`ROCKETMQ_NAME_SERVER` |
| XXL-Job 打不开 | 跑 `init-xxl-job-db.sh`，查 `docker logs cms-xxl-job-admin` |
| `bash\r: No such file or directory` | Windows 换行符问题，见下 |

### 8.1 init 脚本报 `bash\r`

在 Windows 打包的 `.sh` 可能是 CRLF，Linux 上执行：

```bash
cd /opt/cms-middleware
sed -i 's/\r$//' scripts/init-xxl-job-db.sh
chmod +x scripts/init-xxl-job-db.sh
./scripts/init-xxl-job-db.sh
```

或安装 `dos2unix`：`dos2unix scripts/init-xxl-job-db.sh`

### 8.2 MySQL `Access denied for user 'root'`

**1. 先处理 `.env` 换行符（Windows 打包常见）**

```bash
cd /opt/cms-middleware
sed -i 's/\r$//' .env
grep MYSQL_ROOT_PASSWORD .env   # 确认无乱码、无引号
```

**2. 手动测 root 密码**

```bash
docker exec -it cms-mysql mysql -uroot -p
# 输入 .env 里 MYSQL_ROOT_PASSWORD 的值
```

**3. 若仍失败：多半是「先启动了 db，后改的密码」**

MySQL 数据卷 **只在第一次初始化** 时写入 root 密码；之后改 `.env` 不会生效。

- 若这是全新演示机、库里没有重要数据，可重建：

```bash
cd /opt/cms-middleware
docker compose down
docker volume rm cms-middleware-db   # 或 docker volume ls 查实际卷名
docker compose up -d db
# 等 healthy 后再跑 init 脚本
./scripts/init-xxl-job-db.sh
```

- 若记得旧密码，用旧密码跑脚本，或改回 `.env` 与卷一致后再试。

**4. 密码规范**

- 不要加引号：`MYSQL_ROOT_PASSWORD=abc123`（不要 `"abc123"`）
- 避免 `#`、`$`、空格等特殊字符；演示环境用字母数字即可

---

## 9. 下一步

1. 同机或 CI 构建 `cms-back-admin.jar` + systemd。  
2. 构建 `cms-front` 静态资源 + Nginx。  
3. 配置 OSS 写入 `cms-back/.env`。  
4. XXL-Job 控制台配置执行器与清理任务。

---

**文档版本：** v1.2（独立 middleware 部署包）  
**部署包：** `cms-back/deploy/middleware/`  
**维护文档：** `cms-back/deploy/MIDDLEWARE-DEPLOY.md`
