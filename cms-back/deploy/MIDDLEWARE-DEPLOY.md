# CMS 中间件部署教程（Linux / 阿里云轻量 2G / 模拟集群）

**目标：** 用 Docker **拉取官方镜像** 并在服务器上启动 MySQL、Redis、RocketMQ、XXL-Job；**不需要**上传 Java 源码或整个 `cms-back` 工程。

**部署包路径（仓库内）：** `cms-back/deploy/middleware/`  
**服务器推荐目录：** `/opt/cms-middleware`

**范围：** 仅中间件；Spring Boot jar、前端 Nginx 下一步再做。

**推荐部署模式（本文默认）：** 即使目前只有 **一台** 物理机，也按 **「中间件机 + 应用节点」** 分离来部署——应用一律通过 **`MIDDLEWARE_HOST`（私网 IP）** 连中间件，便于以后扩展为多台应用节点，而无需改中间件配置。

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
│   ├── broker-local.conf            # 仅本机 loopback / SSH 隧道开发
│   └── broker-prod.conf             # 模拟集群 / 正式：brokerIP1=私网 IP
├── scripts/
│   └── init-xxl-job-db.sh           # 一次性初始化 xxl_job 库
└── README.md
```

> **本地开发**仍可用仓库根目录 `cms-back/docker-compose.yml`（与 middleware 包等价，路径不同）。**服务器只传 `deploy/middleware` 即可。**

---

## 1. 架构一览

### 1.1 模拟集群（推荐，单机 today，多机 tomorrow）

```text
                    ┌─────────────────────────────────────┐
                    │  中间件机（当前这台轻量服务器）         │
                    │  /opt/cms-middleware                 │
                    │                                      │
                    │  MySQL      :3307                    │
                    │  Redis      :6379                    │
                    │  RocketMQ   :9876 / 10911            │
                    │  XXL-Job    :8088                    │
                    │                                      │
                    │  统一入口：MIDDLEWARE_HOST（私网 IP）  │
                    └──────────────▲──────────────────────┘
                                   │ 同 VPC 私网访问
              ┌────────────────────┼────────────────────┐
              │                    │                    │
       ┌──────┴──────┐      ┌──────┴──────┐     ┌──────┴──────┐
       │ 应用节点 1   │      │ 应用节点 2   │     │ 本地 Dev     │
       │ (同机 jar)  │      │ (将来新机器) │     │ SSH 隧道*)  │
       └─────────────┘      └─────────────┘     └─────────────┘

* 本地开发是例外；测试 / 集群环境不用隧道、不用 127.0.0.1
```

### 1.2 角色与连接方式

| 角色 | 连中间件用什么 | 说明 |
|------|----------------|------|
| **应用节点**（测试 / 生产 / 同机 jar） | `MIDDLEWARE_HOST` = 中间件机 **私网 IP** | 模拟集群、真集群统一约定 |
| **中间件 compose 内部** | Docker 服务名 `db`、`redis` 等 | 仅容器互联，应用不直接用 |
| **本地 Windows Dev** | SSH 隧道 + `127.0.0.1` | 见 §7；不算测试 / 集群方案 |

### 1.3 容器与端口

```text
docker compose
  ├─ cms-mysql          :3307 → 3306
  ├─ cms-redis          :6379
  ├─ cms-rocketmq-namesrv :9876
  ├─ cms-rocketmq-broker  :10911 / 10912 / 10909
  └─ cms-xxl-job-admin    :8088
```

应用侧变量（写入 **`cms-back/.env`**，非 middleware 目录）：

| 变量 | 模拟集群 / 应用节点 | 本地 Dev（SSH 隧道） |
|------|---------------------|----------------------|
| `DB_HOST` / `DB_PORT` | `<私网IP>` / `3307` | `127.0.0.1` / `3307` |
| `REDIS_HOST` / `REDIS_PORT` | `<私网IP>` / `6379` | `127.0.0.1` / `6379` |
| `ROCKETMQ_NAME_SERVER` | `<私网IP>:9876` | `127.0.0.1:9876` |
| `XXL_JOB_ADMIN_ADDRESSES` | `http://<私网IP>:8088/xxl-job-admin` | `http://127.0.0.1:8088/xxl-job-admin` |

---

## 2. 服务器准备

### 2.1 登录并记录私网 IP

```bash
ssh root@<公网IP>
hostname -I
# 或阿里云控制台 → 实例详情 → 私网 IP
```

记下私网 IP，下文称 `<私网IP>`（例如 `172.16.10.5`）。**所有应用节点的 `MIDDLEWARE_HOST` 都填它。**

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
# Swap 不足时可再加 1～2G
```

`.env` 里 RocketMQ 务必：

```bash
ROCKETMQ_NAMESRV_JAVA_OPT=-Xms128m -Xmx256m
ROCKETMQ_BROKER_JAVA_OPT=-Xms128m -Xmx256m
```

### 2.4 防火墙（模拟集群）

- **firewalld 为 inactive**：系统层不拦端口，主要靠 **阿里云轻量「防火墙」** 控制台。
- **模拟集群 / 测试环境（推荐）：**
  - 对公网 **只开 TCP 22**（SSH，建议限源为你的公网 IP）。
  - **不要** 对公网放行 3307、6379、9876、10911、8088。
  - 应用节点通过 **私网 IP** 访问中间件（同 VPC 内）。
- **本地 Windows Dev 连远程中间件：** 不对公网开中间件端口，用 **SSH 隧道**（§7）。

> 填防火墙授权对象时用 **ip.cn 查到的公网 IP**，不要用 `192.168.x.x` 或 `172.20.x.x`（本机/WSL 内网地址无效）。

---

## 3. 上传中间件部署包（不是整仓）

**原则：** 只要服务器上最终有 `/opt/cms-middleware/` 目录里的文件即可。

### 3.1 方式 A — 本机 scp / sftp（常见）

**直接传文件夹：**

```powershell
scp -r D:\code-self\cms\cms-back\deploy\middleware root@<服务器IP>:/opt/cms-middleware
```

**或传已打好的包：**

```powershell
scp middleware.tgz root@<服务器IP>:/tmp/
```

服务器解压：

```bash
sudo mkdir -p /opt
sudo tar -xzf /tmp/middleware.tgz -C /opt
sudo mv /opt/middleware /opt/cms-middleware
cd /opt/cms-middleware
```

### 3.2 方式 B — 图形工具（WinSCP / FileZilla）

1. SFTP 登录服务器，上传 `middleware.tgz` 到 `/tmp/`
2. SSH 解压（见上）

### 3.3 方式 C — 服务器 git clone

```bash
git clone <仓库地址> /tmp/cms-repo
sudo cp -a /tmp/cms-repo/cms-back/deploy/middleware /opt/cms-middleware
```

### 3.4 方式 D — OSS / wget

```bash
cd /tmp && curl -fLO '<下载链接>' -o middleware.tgz
sudo mkdir -p /opt && sudo tar -xzf middleware.tgz -C /opt
sudo mv /opt/middleware /opt/cms-middleware
```

### 3.5 配置 `.env`

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

处理 Windows 打包带来的 CRLF（建议每次上传后执行）：

```bash
sed -i 's/\r$//' .env scripts/*.sh
```

### 3.6 RocketMQ：模拟集群用 `broker-prod.conf`

**模拟集群 / 应用节点通过私网连 Broker 时，不要用 `broker-local.conf`（127.0.0.1）。**

**1. 编辑 prod 配置：**

```bash
vim rocketmq/broker-prod.conf
```

```properties
brokerIP1=<私网IP>    # 与 hostname -I 一致
autoCreateTopicEnable=true
autoCreateSubscriptionGroup=true
```

**2. 修改 `docker-compose.yml` 挂载 prod 配置：**

```yaml
# rocketmq-broker volumes 中改：
- ./rocketmq/broker-prod.conf:/home/rocketmq/conf/broker.conf:ro
```

**3. 若已启动过 broker，改配置后重启：**

```bash
docker compose restart rocketmq-broker
```

| 配置文件 | brokerIP1 | 适用场景 |
|----------|-----------|----------|
| `broker-local.conf` | `127.0.0.1` | 仅 SSH 隧道本地 Dev |
| `broker-prod.conf` | `<私网IP>` | **模拟集群 / 测试 / 生产（推荐）** |

---

## 4. 启动中间件

```bash
cd /opt/cms-middleware

# 1. 基础服务
docker compose up -d db redis
docker compose ps                    # 等 db healthy

# 2. RocketMQ
docker compose up -d rocketmq-namesrv rocketmq-init rocketmq-broker

# 3. 初始化 XXL-Job 库（一次性）
sed -i 's/\r$//' scripts/init-xxl-job-db.sh
chmod +x scripts/init-xxl-job-db.sh
./scripts/init-xxl-job-db.sh

# 4. 全部拉起
docker compose up -d xxl-job-admin
# 或：docker compose up -d

docker compose ps
```

除一次性容器 `cms-rocketmq-init` 外，其余应为 `running` / `healthy`。

---

## 5. 验收

### 5.1 容器内快速检查

```bash
cd /opt/cms-middleware
set -a && source .env && set +a

docker exec -it cms-mysql mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SHOW DATABASES;"
docker exec -it cms-redis redis-cli ping
docker logs cms-rocketmq-broker --tail 30
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8088/xxl-job-admin/
```

### 5.2 模拟集群：用私网 IP 验收（重要）

**故意不用 `127.0.0.1`**，验证「应用节点连中间件机」链路：

```bash
# MySQL（需本机已装 mysql 客户端，或在另一台 VPC 机器上测）
mysql -h <私网IP> -P 3307 -ucms -p -e "SELECT 1"

# Redis
redis-cli -h <私网IP> -p 6379 ping

# XXL-Job
curl -s -o /dev/null -w "%{http_code}\n" http://<私网IP>:8088/xxl-job-admin/
```

### 5.3 XXL-Job 控制台

- 同 VPC / 同机：`http://<私网IP>:8088/xxl-job-admin`
- 外网运维：SSH 隧道 `-L 8088:127.0.0.1:8088` 后访问 `http://127.0.0.1:8088/xxl-job-admin`
- 默认账号：`admin` / `123456`，登录后改密码

---

## 6. 应用节点连接（模拟集群 / 测试环境）

middleware 目录的 `.env` **只给 compose 用**。应用在 **`cms-back/.env`**（测试机 jar 目录）里配置：

```bash
# 中间件机私网 IP（核心变量，集群扩展时不变）
DB_HOST=<私网IP>
DB_PORT=3307
DB_NAME=cms
DB_USERNAME=cms
DB_PASSWORD=与 middleware/.env 的 MYSQL_PASSWORD 一致

REDIS_HOST=<私网IP>
REDIS_PORT=6379

ROCKETMQ_NAME_SERVER=<私网IP>:9876

XXL_JOB_ADMIN_ADDRESSES=http://<私网IP>:8088/xxl-job-admin
XXL_JOB_EXECUTOR_PORT=9999
# executor ip 留空 → 自动注册本机私网 IP
```

**Liquibase：** 开发 / 测试首次启动应用（非 `prod` profile）时，会在远程 `cms` 库自动建表，无需手工导 SQL。

**同机启动 jar 示例：**

```bash
cd /path/to/cms-back
set -a && source .env && set +a
java -Xms256m -Xmx512m -jar cms-back-admin/target/cms-back-admin-*.jar
```

### 6.1 模拟两台应用节点（可选，同一台物理机）

```bash
java -jar cms-back-admin.jar --server.port=8080
java -jar cms-back-admin.jar --server.port=8081
```

同一执行器 AppName（`cms-back-admin-executor`）下会出现 2 台机器，XXL-Job 按路由策略分发。

### 6.2 以后扩展真集群

- **中间件机：** 配置不变（或 MySQL 迁 RDS，只改 JDBC 地址）。
- **新增应用机 2、3：** 复制同一套 `cms-back/.env`，`DB_HOST` 等仍指向中间件 `<私网IP>`。
- **RocketMQ / XXL-Job：** 无需为每台应用机单独改中间件。

---

## 7. 本地 Windows 开发：SSH 隧道（仅 Dev）

测试 / 集群环境 **不用** 本节；仅「本机 Dev + 远程测试中间件」时使用。防火墙 **只开 22**，中间件端口不对公网暴露。

### 7.1 隧道命令（窗口保持打开）

```powershell
ssh -N -o ServerAliveInterval=60 `
  -L 3307:127.0.0.1:3307 `
  -L 6379:127.0.0.1:6379 `
  -L 9876:127.0.0.1:9876 `
  -L 10911:127.0.0.1:10911 `
  -L 10909:127.0.0.1:10909 `
  -L 10912:127.0.0.1:10912 `
  -L 8088:127.0.0.1:8088 `
  root@<公网IP>
```

输完密码后窗口「卡住不动」= 隧道正常。另开窗口继续开发。

验证：

```powershell
Test-NetConnection 127.0.0.1 -Port 3307
```

### 7.2 本地 `cms-back/.env`

```bash
DB_HOST=127.0.0.1
REDIS_HOST=127.0.0.1
ROCKETMQ_NAME_SERVER=127.0.0.1:9876
XXL_JOB_ADMIN_ADDRESSES=http://127.0.0.1:8088/xxl-job-admin
```

服务器上 RocketMQ 若走隧道 Dev，需临时改回 `broker-local.conf`（`brokerIP1=127.0.0.1`）并重启 broker；**测试 / 集群环境用 `broker-prod.conf`，二者不要混在同一套中间件上同时服务。**

### 7.3 Dev 与测试执行器隔离（建议）

| 环境 | 连中间件 | 执行器 AppName |
|------|----------|----------------|
| 本地 Dev | SSH 隧道 + `127.0.0.1` | `cms-back-admin-executor-dev`（在 `application-local.yml` 覆盖） |
| 测试 / 集群 | `<私网IP>` | `cms-back-admin-executor` |

避免本地与测试机抢同一批定时任务。

### 7.4 DBeaver

- 先开隧道，再连：主机 `127.0.0.1`，端口 `3307`，库 `cms`，用户 `cms`。

---

## 8. XXL-Job（集群模式，任务只配一次）

任务配置存在 **`xxl_job` 库**，与代码中的 `@XxlJob("handlerName")` 绑定。**发布新版本 jar 一般不用重建任务**；仅 **新增 Handler** 时在控制台加一条。

### 8.1 执行器（测试 / 集群）

1. 打开 Admin（`<私网IP>:8088` 或 SSH 隧道 `127.0.0.1:8088`）
2. **执行器管理 → 新增**
   - AppName：`cms-back-admin-executor`（与 `application.yml` 一致）
   - 注册方式：**自动注册**
3. 启动应用后，应看到应用机 **私网 IP:9999** 在线

### 8.2 新建任务（本项目已有 Handler）

| JobHandler | 说明 |
|------------|------|
| `mediaStaleUploadingCleanupJob` | 清理超时 uploading |
| `mediaOrphanReadyCleanupJob` | 清理无引用 ready |

**任务管理 → 新增：** 执行器选 `cms-back-admin-executor`，运行模式 **BEAN**，Cron 自定，JobHandler 填上表名称。

### 8.3 本地 Dev 测定时任务（可选，较麻烦）

Admin 在服务器、执行器在本机时，调度中心需 **回调本机 9999**，需反向隧道 `-R 9999:127.0.0.1:9999` 及服务器 socat 转发。演示阶段建议：**定时任务在测试环境验**，本地只开发业务逻辑。

---

## 9. 运维速查

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

## 10. 常见问题

| 现象 | 处理 |
|------|------|
| OOM / 卡死 | 减小 RocketMQ 堆、加 Swap、`docker stats` |
| `dnf install docker` 冲突 | 用 Docker CE，见 §2.2 |
| RocketMQ 连不上 | 检查 `brokerIP1` 与场景是否匹配（prod=私网 IP，local=127.0.0.1） |
| XXL-Job 打不开 | 跑 `init-xxl-job-db.sh`，查 `docker logs cms-xxl-job-admin` |
| DBeaver `Connect timed out` | 公网直连需防火墙；Dev 应走 SSH 隧道 + `127.0.0.1` |
| 防火墙填 `192.168.x.x` 无效 | 必须用 ip.cn 公网 IP；内网地址云防火墙不认 |
| `bash\r: No such file or directory` | 见 §10.1 |
| MySQL `Access denied` | 见 §10.2 |

### 10.1 init 脚本报 `bash\r`

```bash
cd /opt/cms-middleware
sed -i 's/\r$//' scripts/init-xxl-job-db.sh .env
chmod +x scripts/init-xxl-job-db.sh
./scripts/init-xxl-job-db.sh
```

### 10.2 MySQL `Access denied for user 'root'`

**1. 处理 `.env` CRLF：** `sed -i 's/\r$//' .env`

**2. 手动测密码：** `docker exec -it cms-mysql mysql -uroot -p`

**3. 先启动 db 后改密码：** MySQL 卷只在首次初始化写密码。演示机可重建卷：

```bash
docker compose down
docker volume rm cms-middleware-db
docker compose up -d db
./scripts/init-xxl-job-db.sh
```

**4. 密码不要加引号**；演示环境尽量字母数字。

---

## 11. 部署检查清单（模拟集群）

```text
□ 查私网 IP → <私网IP>
□ /opt/cms-middleware 配置 .env（密码、RocketMQ 小堆）
□ broker-prod.conf：brokerIP1=<私网IP>
□ docker-compose.yml 挂载 broker-prod.conf
□ docker compose up（db → init xxl_job → 全量）
□ 用 <私网IP> 验收 MySQL / Redis / XXL-Job
□ 应用 cms-back/.env 全部指向 <私网IP>
□ 防火墙：中间件端口不对公网开；SSH 22 可运维
□ XXL-Job：自动注册执行器 + 建任务（一次性）
□ 本地 Dev：SSH 隧道 + executor-dev（与测试隔离）
```

---

## 12. 下一步

1. 同机或 CI 构建 `cms-back-admin.jar` + systemd。  
2. 构建 `cms-front` 静态资源 + Nginx。  
3. 配置 OSS 写入 `cms-back/.env`。  
4. XXL-Job 控制台配置清理任务（若 §8 未做）。

---

**文档版本：** v1.3（模拟集群 + SSH Dev 隧道）  
**部署包：** `cms-back/deploy/middleware/`  
**维护文档：** `cms-back/deploy/MIDDLEWARE-DEPLOY.md`
