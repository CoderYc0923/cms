# 中间件独立部署包（仅 Docker，不含 Java 源码）
#
# 上传到服务器示例目录：/opt/cms-middleware
#
#   /opt/cms-middleware/
#     docker-compose.yml
#     .env                 ← 从 .env.example 复制
#     rocketmq/broker-local.conf
#     scripts/init-xxl-job-db.sh
#
# 启动：docker compose up -d
#
# 详细步骤见：../MIDDLEWARE-DEPLOY.md
