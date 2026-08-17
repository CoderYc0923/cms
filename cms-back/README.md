# 本地
mvn spring-boot:run -pl cms-back-admin "-Dspring-boot.run.profiles=local"

# 或 IDE Active profiles = local

# 模拟生产日志格式
mvn spring-boot:run -pl cms-back-admin "-Dspring-boot.run.profiles=prod"