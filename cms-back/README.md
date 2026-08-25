# 本地（在 cms-back 目录执行；会自动加载同目录 .env）
mvn spring-boot:run -pl cms-back-admin -am "-Dspring-boot.run.profiles=local"
# 或 IDE Active profiles = local

# 模拟生产日志格式
mvn spring-boot:run -pl cms-back-admin "-Dspring-boot.run.profiles=prod"

# Xxl-job 初始化
1. 启动mysql
2. 初始化xxl_job库
```bash
# 从官网下载对应的sql
cd D:\code-self\cms\cms-back
Invoke-WebRequest -Uri "https://raw.githubusercontent.com/xuxueli/xxl-job/2.4.2/doc/db/tables_xxl_job.sql" -OutFile ".\deploy\xxl-job\tables_xxl_job.sql"
(Get-Content .\deploy\xxl-job\tables_xxl_job.sql).Count
```
./deploy/xxl-job/init-xxl-job-db.sh   # 或 ps1
3. 启动调度中心
4. 浏览器操作
浏览器登录改密码（默认 admin / 123456） → 配执行器 → 建任务