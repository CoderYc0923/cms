#!/usr/bin/env bash
# 初始化 xxl_job 库（在 middleware 目录执行）
#   chmod +x scripts/init-xxl-job-db.sh
#   ./scripts/init-xxl-job-db.sh

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env"
SQL_FILE="$ROOT_DIR/scripts/tables_xxl_job.sql"

get_env() {
  local key="$1" default="$2"
  if [[ -f "$ENV_FILE" ]]; then
    local val
    val="$(grep -E "^${key}=" "$ENV_FILE" | head -n1 | cut -d= -f2-)"
    if [[ -n "$val" ]]; then echo "$val"; return; fi
  fi
  echo "$default"
}

MYSQL_CONTAINER="cms-mysql"
ROOT_PASSWORD="$(get_env MYSQL_ROOT_PASSWORD "")"
XXL_VERSION="$(get_env XXL_JOB_ADMIN_IMAGE_TAG "2.4.2")"

if [[ -z "$ROOT_PASSWORD" ]]; then
  echo "请在 $ENV_FILE 配置 MYSQL_ROOT_PASSWORD" >&2
  exit 1
fi

if ! docker ps --format '{{.Names}}' | grep -qx "$MYSQL_CONTAINER"; then
  echo "容器 $MYSQL_CONTAINER 未运行，请先: cd $ROOT_DIR && docker compose up -d db" >&2
  exit 1
fi

EXISTS="$(docker exec "$MYSQL_CONTAINER" mysql -uroot -p"$ROOT_PASSWORD" -N -e \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='xxl_job' AND table_name='xxl_job_group';")"
if [[ "$EXISTS" == "1" ]]; then
  echo "[skip] xxl_job 已存在 xxl_job_group，跳过初始化"
  exit 0
fi

echo "[1/4] 创建数据库 xxl_job ..."
docker exec -i "$MYSQL_CONTAINER" mysql -uroot -p"$ROOT_PASSWORD" -e \
  "CREATE DATABASE IF NOT EXISTS xxl_job DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

echo "[2/4] 下载 tables_xxl_job.sql (tag=$XXL_VERSION) ..."
curl -fsSL "https://raw.githubusercontent.com/xuxueli/xxl-job/${XXL_VERSION}/doc/db/tables_xxl_job.sql" -o "$SQL_FILE"

echo "[3/4] 导入表结构 ..."
docker exec -i "$MYSQL_CONTAINER" mysql -uroot -p"$ROOT_PASSWORD" xxl_job < "$SQL_FILE"

echo "[4/4] 验收 ..."
docker exec -i "$MYSQL_CONTAINER" mysql -uroot -p"$ROOT_PASSWORD" -e "USE xxl_job; SHOW TABLES;"

echo "完成。可执行: cd $ROOT_DIR && docker compose up -d xxl-job-admin"
