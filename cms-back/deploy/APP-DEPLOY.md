# CMS 后端应用部署（jar + systemd）

**前提：** 中间件已按 [MIDDLEWARE-DEPLOY.md](./MIDDLEWARE-DEPLOY.md) 在 `/opt/cms-middleware` 跑通。

**本文：** 在阿里云轻量服务器上部署 `/opt/cms-back` 下的 Spring Boot jar。

---

## 1. 目录约定

```text
/opt/cms-middleware/     # Docker 中间件（MySQL / Redis / RocketMQ / XXL-Job）
/opt/cms-back/           # 应用
├── cms-back-admin.jar   # 你上传的 jar（名称可自定，与 systemd 一致即可）
├── .env                 # 应用环境变量（必建）
└── logs/                # 运行日志
```

---

## 2. 确认中间件正常

```bash
cd /opt/cms-middleware
docker compose ps
# db、redis、rocketmq-*、xxl-job-admin 应为 running / healthy
```

记录私网 IP（同机部署也建议用私网 IP，与中间件文档一致）：

```bash
hostname -I | awk '{print $1}'
# 下文称 <私网IP>
```

---

## 3. 准备 jar 与目录

若 jar 名带版本号，建议固定软链，方便 systemd：

```bash
cd /opt/cms-back
ls -la
# 例如实际文件 cms-back-admin-0.0.1-SNAPSHOT.jar
ln -sf cms-back-admin-0.0.1-SNAPSHOT.jar cms-back-admin.jar

mkdir -p logs logs/xxl-job
chmod 700 /opt/cms-back
```

---

## 4. 配置 `.env`

```bash
cd /opt/cms-back
cp /path/to/repo/cms-back/deploy/app/.env.example .env
# 或手动 vim .env
chmod 600 .env
sed -i 's/\r$//' .env
vim .env
```

**必改项：**

| 变量 | 说明 |
|------|------|
| `DB_HOST` | 中间件机私网 IP（同机填 `<私网IP>`，不要填公网） |
| `DB_PASSWORD` | 与 `/opt/cms-middleware/.env` 的 `MYSQL_PASSWORD` **一致** |
| `REDIS_HOST` | 同 `DB_HOST` |
| `ROCKETMQ_NAME_SERVER` | `<私网IP>:9876` |
| `XXL_JOB_ADMIN_ADDRESSES` | `http://<私网IP>:8088/xxl-job-admin` |
| `CMS_JWT_SECRET` | 至少 32 位随机字符串 |
| `OSS_*` | 阿里云 OSS 四项 |

同机示例：

```bash
DB_HOST=172.16.10.5
DB_PORT=3307
DB_NAME=cms
DB_USERNAME=cms
DB_PASSWORD=你的middleware密码

REDIS_HOST=172.16.10.5
REDIS_PORT=6379

ROCKETMQ_NAME_SERVER=172.16.10.5:9876
XXL_JOB_ADMIN_ADDRESSES=http://172.16.10.5:8088/xxl-job-admin
XXL_JOB_LOG_PATH=/opt/cms-back/logs/xxl-job

CMS_JWT_SECRET=随机长字符串至少32字符
OSS_ENDPOINT=https://oss-cn-xxx.aliyuncs.com
OSS_BUCKET=xxx
OSS_ACCESS_KEY_ID=xxx
OSS_ACCESS_KEY_SECRET=xxx
```

应用会从 **jar 同级** 的 `.env` 读取（见 `application.yml` 的 `optional:file:.env`）。

---

## 5. 首次启动：建表（Liquibase）

`prod` profile **不会**自动跑迁移（见 `application-prod.yml`）。首次部署二选一：

### 方式 A — 先不用 prod 启动一次（推荐）

```bash
cd /opt/cms-back
set -a && source .env && set +a

java -Xms256m -Xmx512m -jar cms-back-admin.jar
# 看到 Started ... 且无 Liquibase 报错后 Ctrl+C
```

确认表已创建：

```bash
docker exec -it cms-mysql mysql -ucms -p -e "USE cms; SHOW TABLES;"
```

### 方式 B — 本机 Maven 对远程库执行

在开发机 `cms-back` 目录配置相同 JDBC 后：

```bash
mvn -pl cms-back-admin liquibase:update
```

---

## 6. 安装 systemd（开机自启）

```bash
# 复制 unit（仓库 deploy/app/cms-back.service，或按下面手动写）
sudo cp deploy/app/cms-back.service /etc/systemd/system/cms-back.service

# 若 jar 名不是 cms-back-admin.jar，编辑 ExecStart 路径
sudo vim /etc/systemd/system/cms-back.service

sudo systemctl daemon-reload
sudo systemctl enable cms-back
sudo systemctl start cms-back
sudo systemctl status cms-back
```

查看日志：

```bash
tail -f /opt/cms-back/logs/app.log
journalctl -u cms-back -f
```

---

## 7. 验收

```bash
# 健康检查（默认 8080）
curl -s http://127.0.0.1:8080/actuator/health

# 公网访问需在阿里云轻量「防火墙」放行 TCP 8080（或只通过 Nginx 反代，见下）
```

**XXL-Job 执行器：**

1. 浏览器打开 `http://<私网IP>:8088/xxl-job-admin`（或 SSH 隧道）
2. **执行器管理** 中应出现 `cms-back-admin-executor`，地址为应用机私网 IP:9999
3. 若无，检查应用日志里 xxl-job 相关报错、9999 端口是否监听：`ss -tlnp | grep 9999`

**定时任务（一次性）：** 见 [MIDDLEWARE-DEPLOY.md §8](./MIDDLEWARE-DEPLOY.md#8-xxl-job集群模式任务只配一次)。

---

## 8. 防火墙与对外访问

| 端口 | 建议 |
|------|------|
| 22 | 运维 SSH，限源 IP |
| 8080 | 仅内网 / Nginx 反代；**不要**对公网裸奔 |
| 3307/6379/9876/8088 | **不对公网开放**（中间件文档已说明） |

前端 + GitHub Actions 见 **[CI-CD.md](./CI-CD.md)**（Nginx 完整配置：`nginx/cms-front.conf`）。

API 反代核心：

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

---

## 9. 更新 jar

```bash
cd /opt/cms-back
# 上传新 jar 后
ln -sf cms-back-admin-新版本.jar cms-back-admin.jar
sudo systemctl restart cms-back
tail -f logs/app.log
```

---

## 10. 常见问题

| 现象 | 处理 |
|------|------|
| `Access denied for user 'cms'` | `.env` 密码与 middleware 不一致；检查 CRLF |
| 连不上 Redis/MySQL | `DB_HOST` 应用私网 IP；`docker compose ps` 确认中间件 up |
| RocketMQ 超时 | `broker-prod.conf` 的 `brokerIP1` 是否为私网 IP |
| OSS 上传失败 | 补全 `OSS_*`；Bucket CORS |
| prod 启动但无表 | 先按 §5 跑 Liquibase |
| OOM | 减小 `-Xmx` 或加 Swap |

---

## 11. 检查清单

```text
□ 中间件 docker compose ps 正常
□ /opt/cms-back/.env 已配置（DB/Redis/RocketMQ/JWT/OSS）
□ 首次 Liquibase 建表完成
□ systemd start 成功，health 200
□ XXL-Job 执行器在线
□ 防火墙：8080 按需开放或走 Nginx
```

---

**维护：** `cms-back/deploy/APP-DEPLOY.md`  
**关联：** `cms-back/deploy/MIDDLEWARE-DEPLOY.md`
