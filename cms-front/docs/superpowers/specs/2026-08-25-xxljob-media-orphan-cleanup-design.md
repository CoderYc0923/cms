# XXL-Job 媒体孤儿文件定时清理设计（手敲教程 · 完整版）

> 日期：2026-08-25（修订）  
> 状态：待手敲落地  
> 执行器：`cms-back-admin`（与 Spring Boot 同进程）  
> 调度：XXL-Job Admin（Docker Compose）  
> 对齐现状：`media_files` / `article_media_refs` 字段、`OssStorage`、`application.yml` 的 `cms.media-cleanup`

---

## 0. 文档怎么用

按顺序手敲，**不要跳步**：

1. §1～§3 搞清要清什么、为什么  
2. §4 依赖（父 POM 已有版本时可跳过版本声明，核对 groupId）  
3. §5～§6 Docker Admin + 建库初始化（**先跑通控制台**）  
4. §7～§8 `.env` + `application.yml`  
5. §9～§12 Java / Mapper / Service / Handler  
6. §13 控制台建执行器与任务  
7. §14 dry-run 验证 → §15 正式跑

---

## 1. 目标与非目标

### 1.1 目标

| Job | Handler 名（控制台 JobHandler） | 清理对象 | 动作 |
|---|---|---|---|
| A | `mediaStaleUploadingCleanupJob` | `status=uploading` 且超过宽限期 | 有 `upload_id` → `abortMultipart`；否则 `deleteObject` → 库状态改 `failed` |
| B | `mediaOrphanReadyCleanupJob` | `status=ready` + `access_level=private` + **无** `article_media_refs` + 超过宽限期 | `deleteObject` → 库状态改 `deleted` |

### 1.2 非目标

- **不删** `access_level=public` 的文件（即使暂时无 ref）  
- **不扫** `articles.content`（以 `article_media_refs` 为准）  
- **不物理 DELETE** `media_files` 行（只改 `status`）  
- **不**在高峰硬性要求必须深夜跑（宽限期才是正确性保证；凌晨是优化）

### 1.3 与现有表字段对齐（重要）

`media_files`（005）：

| 列 | 实体字段 | 说明 |
|---|---|---|
| `object_key` | `objectKey` | OSS 路径 |
| `upload_id` | `uploadId` | 分片 uploadId，完成后可空 |
| `access_level` | `accessLevel` | `private` / `public` |
| `status` | `status` | `uploading` / `ready` / `failed` / `deleted` |
| `created_at` | `createdAt` | 宽限期基准 |

`article_media_refs`（006）：`article_id` + `file_id`，唯一索引 `(article_id, file_id)`。

`OssStorage` 已有：`abortMultipart(objectKey, uploadId)`、`deleteObject(objectKey)`。

---

## 2. 为什么用 XXL-Job

| 能力 | `@Scheduled` | XXL-Job |
|---|---|---|
| 改 Cron 不发版 | 难 | 控制台改 |
| 手动触发 / 看执行日志 | 弱 | 强 |
| 多实例防重复 | 要自己做 | 路由 + 阻塞策略 |
| 失败告警 | 自己接 | 控制台可配 |

本项目：**调度中心独立 Docker**；**执行器嵌在 `cms-back-admin`**（本机 IDE 或以后容器化应用均可）。

---

## 3. 架构

```text
浏览器 → XXL-Job Admin :8088
              │ 调度 HTTP
              ▼
     cms-back-admin（本机 / 容器）
       ├─ XxlJobConfig（注册执行器）
       ├─ MediaCleanupJobHandler  @XxlJob("...")
       └─ MediaCleanupService
              ├─ MediaFilesMapper（查候选）
              └─ OssStorage（abort / delete）
                    │
         MySQL: media_files / article_media_refs
         OSS: 桶内 object_key
```

**网络注意：**

| 谁连谁 | 正确地址 |
|---|---|
| Admin 容器 → MySQL | `db:3306`（服务名），**不要** `localhost` |
| Admin 容器 → 库名 | 建议独立库 **`xxl_job`**，不要用业务库 `cms` |
| 本机 Spring 执行器 → Admin | `http://127.0.0.1:8088/xxl-job-admin` |
| 宿主机访问 MySQL | `127.0.0.1:3307`（映射口） |

---

## 4. Maven 依赖

### 4.1 父 POM（你已加可核对）

`cms-back/pom.xml`：

```xml
<properties>
    <xxl-job.version>2.4.2</xxl-job.version>
</properties>

<!-- dependencyManagement -->
<dependency>
    <groupId>com.xuxueli</groupId>
    <artifactId>xxl-job-core</artifactId>
    <version>${xxl-job.version}</version>
</dependency>
```

> 官方坐标是 **`com.xuxueli`**（不是 `com.xuxueli` 拼错）。若 `admin/pom.xml` 写成错 groupId，Maven 拉不下来。

### 4.2 `cms-back-admin/pom.xml`

```xml
<dependency>
    <groupId>com.xuxueli</groupId>
    <artifactId>xxl-job-core</artifactId>
</dependency>
```

**落点约定：**

| 代码 | 模块 |
|---|---|
| `XxlJobConfig`、`MediaCleanupJobHandler` | `cms-back-admin` |
| `MediaCleanupProperties`、`MediaCleanupService`、Mapper 方法 | `cms-back-system` |

---

## 5. 宽限期策略（写在哪里）

### 5.1 配置值写在

**`cms-back-admin/src/main/resources/application.yml`**（你已有一版，保持键名一致）：

```yaml
cms:
  media-cleanup:
    enabled: true
    batch-size: 200
    stale-uploading-after-hours: 24   # uploading 超时
    orphan-ready-after-days: 7        # ready 无 ref 超时
    dry-run: false                    # 联调先改 true
```

**不要**把宽限期塞进 `.env`（除非你想用 `${ENV}` 覆盖）；`.env` 只管 Docker / 地址类变量（§7）。

生产可在 `application-prod.yml` 覆盖：

```yaml
cms:
  media-cleanup:
    dry-run: false
    orphan-ready-after-days: 7
```

### 5.2 参数含义

| 配置键 | Java 字段 | 建议 | 作用 |
|---|---|---|---|
| `enabled` | `enabled` | true | 总开关；false 则 Job 直接 return 0 |
| `batch-size` | `batchSize` | 200 | 每批最多处理条数 |
| `stale-uploading-after-hours` | `staleUploadingAfterHours` | 24 | `created_at < now - 24h` 且 uploading |
| `orphan-ready-after-days` | `orphanReadyAfterDays` | 7 | 防「上传完还没点保存」误删 |
| `dry-run` | `dryRun` | 先 true | true：只打日志，不调 OSS、不改库 |

### 5.3 为何 7 天

```text
用户：complete 成功 → 编辑器里写着 → 还没 save
      此时 article_media_refs 还没有该 file_id
```

若「无 ref 就删」，会删掉正在编辑的图。宽限期 ≥ 编辑会话最大空闲时间（建议 7 天）。

---

## 6. Docker：XXL-Job Admin（修正版）

你当前 `docker-compose.yml` 有几处必须改，否则 Admin 起不来或连不上库。

### 6.1 问题清单

| 问题 | 现象 | 改法 |
|---|---|---|
| `image: xuxueli/xxl-job-admin: ${TAG}` 冒号后有空格 | 镜像名非法 | 去掉空格 |
| JDBC 用 `localhost` / 宿主机 `3307` | 容器内连不到 MySQL | 用 **`db:3306`** |
| 库名用业务库 `cms` | 污染业务库、表冲突 | 独立库 **`xxl_job`** |
| 未执行官方建表 SQL | Admin 启动报缺表 | 见 §6.3 |

### 6.2 推荐 `docker-compose.yml` 片段（替换现有 xxl-job-admin）

```yaml
  xxl-job-admin:
    image: xuxueli/xxl-job-admin:${XXL_JOB_ADMIN_IMAGE_TAG:-2.4.2}
    container_name: cms-xxl-job-admin
    restart: unless-stopped
    ports:
      - "${XXL_JOB_ADMIN_PORT:-8088}:8080"
    environment:
      # 官方镜像用 PARAMS 追加 Spring 参数
      PARAMS: >-
        --spring.datasource.url=jdbc:mysql://db:3306/xxl_job?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
        --spring.datasource.username=root
        --spring.datasource.password=${MYSQL_ROOT_PASSWORD}
    depends_on:
      db:
        condition: service_healthy
```

说明：

- 用 **root** 连 `xxl_job`（业务用户 `cms` 默认对 `xxl_job` 无权限）；也可用授权后的专用账号。  
- 宿主机映射 **8088→8080**（容器内 Admin 端口是 8080）。

### 6.3 一次性初始化 `xxl_job` 库

在 MySQL 起来后（宿主机连 `127.0.0.1:3307`）：

```sql
CREATE DATABASE IF NOT EXISTS xxl_job
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

下载官方脚本并执行（版本与镜像一致，如 2.4.2）：

- GitHub：`https://github.com/xuxueli/xxl-job/blob/master/doc/db/tables_xxl_job.sql`  
- 或 release 包里的 `tables_xxl_job.sql`

```bash
# 示例：在 cms-back 目录
docker exec -i cms-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" xxl_job < tables_xxl_job.sql
```

### 6.4 启动与验收

```bash
cd cms-back
docker compose up -d db xxl-job-admin
docker compose logs -f xxl-job-admin
```

浏览器：`http://127.0.0.1:8088/xxl-job-admin`  
默认账号：`admin` / `123456`（上线务必改）。

---

## 7. `.env` 该配什么

文件：`cms-back/.env`（给 **docker compose** + 可选给本机应用）。

### 7.1 已有（给 db）

```env
MYSQL_DATABASE=cms
MYSQL_USER=cms
MYSQL_PASSWORD=...
MYSQL_ROOT_PASSWORD=...
MYSQL_PORT=3307
```

### 7.2 建议新增（给 xxl-job-admin）

```env
# XXL-Job Admin 容器
XXL_JOB_ADMIN_PORT=8088
XXL_JOB_ADMIN_IMAGE_TAG=2.4.2
```

### 7.3 建议新增（给本机跑的 Spring 执行器）

应用在 IDE 启动、不在 Docker 里时，需要连宿主机上的 Admin：

```env
XXL_JOB_ADMIN_ADDRESSES=http://127.0.0.1:8088/xxl-job-admin
XXL_JOB_EXECUTOR_PORT=9999
XXL_JOB_ACCESS_TOKEN=
XXL_JOB_LOG_PATH=./logs/xxl-job
```

### 7.4 不要写进 `.env` 的

`cms.media-cleanup.*`（宽限期）→ 写在 **`application.yml`**。

---

## 8. `application.yml` 完整 XXL + cleanup 段

在 `cms-back-admin/src/main/resources/application.yml` 追加（`cms.media-cleanup` 你已有可保留）：

```yaml
xxl:
  job:
    admin:
      addresses: ${XXL_JOB_ADMIN_ADDRESSES:http://127.0.0.1:8088/xxl-job-admin}
    executor:
      appname: cms-back-admin-executor
      address: ""
      ip: ""
      port: ${XXL_JOB_EXECUTOR_PORT:9999}
      logpath: ${XXL_JOB_LOG_PATH:./logs/xxl-job}
      logretentiondays: 30
    accessToken: ${XXL_JOB_ACCESS_TOKEN:}

cms:
  media-cleanup:
    enabled: true
    batch-size: 200
    stale-uploading-after-hours: 24
    orphan-ready-after-days: 7
    dry-run: true   # 首次联调务必 true
```

IDE 启动时：确保能读到 `.env`，或在 Run Configuration 里配同样环境变量。  
若项目用 `dotenv` / 启动脚本加载 `.env`，与现有 `DB_*` 方式保持一致即可。

---

## 9. Properties + XxlJobConfig

### 9.1 `MediaCleanupProperties.java`

路径：`cms-back-system/src/main/java/com/cms/cms_back/system/media/MediaCleanupProperties.java`

```java
package com.cms.cms_back.system.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "cms.media-cleanup")
public class MediaCleanupProperties {

    /** 总开关 */
    private boolean enabled = true;

    /** 每批最多处理条数 */
    private int batchSize = 200;

    /** uploading 超过多少小时视为卡住 */
    private int staleUploadingAfterHours = 24;

    /** ready 无引用超过多少天视为孤儿 */
    private int orphanReadyAfterDays = 7;

    /** true：只打日志，不删 OSS、不改库 */
    private boolean dryRun = false;
}
```

### 9.2 `XxlJobConfig.java`

路径：`cms-back-admin/src/main/java/com/cms/cms_back/admin/config/XxlJobConfig.java`

```java
package com.cms.cms_back.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cms.cms_back.system.media.MediaCleanupProperties;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;

@Configuration
@EnableConfigurationProperties(MediaCleanupProperties.class)
public class XxlJobConfig {

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.executor.appname}")
    private String appname;

    @Value("${xxl.job.executor.address:}")
    private String address;

    @Value("${xxl.job.executor.ip:}")
    private String ip;

    @Value("${xxl.job.executor.port}")
    private int port;

    @Value("${xxl.job.executor.logpath}")
    private String logPath;

    @Value("${xxl.job.executor.logretentiondays}")
    private int logRetentionDays;

    @Value("${xxl.job.accessToken:}")
    private String accessToken;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAppname(appname);
        executor.setAddress(address);
        executor.setIp(ip);
        executor.setPort(port);
        executor.setLogPath(logPath);
        executor.setLogRetentionDays(logRetentionDays);
        executor.setAccessToken(accessToken);
        return executor;
    }
}
```

启动后日志应出现执行器注册成功；控制台「执行器管理」能看到机器地址。

---

## 10. Mapper

### 10.1 `MediaFilesMapper.java` 追加

路径：已有 `cms-back-system/.../mapper/MediaFilesMapper.java`

```java
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Param;

List<MediaFiles> selectStaleUploading(@Param("deadline") LocalDateTime deadline,
                                      @Param("limit") int limit);

List<MediaFiles> selectOrphanReadyPrivate(@Param("deadline") LocalDateTime deadline,
                                          @Param("limit") int limit);
```

### 10.2 `MediaFilesMapper.xml`

路径：`cms-back-system/src/main/resources/mapper/MediaFilesMapper.xml`  
（与现有 `mybatis-plus.mapper-locations: classpath*:/mapper/**/*.xml` 一致）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.cms.cms_back.system.mapper.MediaFilesMapper">

    <!-- 超时仍在 uploading 的记录 -->
    <select id="selectStaleUploading" resultType="com.cms.cms_back.pojo.entity.MediaFiles">
        SELECT id, object_key, upload_id, status, access_level, created_at
        FROM media_files
        WHERE status = 'uploading'
          AND created_at &lt; #{deadline}
        ORDER BY created_at ASC
        LIMIT #{limit}
    </select>

    <!-- ready + private + 无任何文章引用 + 超过宽限期 -->
    <select id="selectOrphanReadyPrivate" resultType="com.cms.cms_back.pojo.entity.MediaFiles">
        SELECT mf.id, mf.object_key, mf.upload_id, mf.status, mf.access_level, mf.created_at
        FROM media_files mf
        WHERE mf.status = 'ready'
          AND mf.access_level = 'private'
          AND mf.created_at &lt; #{deadline}
          AND NOT EXISTS (
              SELECT 1
              FROM article_media_refs r
              WHERE r.file_id = mf.id
          )
        ORDER BY mf.created_at ASC
        LIMIT #{limit}
    </select>

</mapper>
```

> 列名用 **`object_key` / `upload_id` / `access_level`**，与 005 DDL 一致（不是 `object_key` 写成 `objectKey`）。

可选索引（数据量大再加 Liquibase 007）：

```sql
KEY idx_media_files_status_created (status, created_at)
```

---

## 11. Service

### 11.1 接口

路径：`cms-back-system/.../service/MediaCleanupService.java`

```java
package com.cms.cms_back.system.service;

public interface MediaCleanupService {

    /** @return 成功处理条数（dry-run 也计为「处理」或按你约定返回候选数） */
    int cleanupStaleUploading();

    int cleanupOrphanReady();
}
```

### 11.2 实现

路径：`cms-back-system/.../service/serviceImpl/MediaCleanupServiceImpl.java`

```java
package com.cms.cms_back.system.service.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cms.cms_back.pojo.entity.MediaFiles;
import com.cms.cms_back.pojo.enums.MediaFilesStatus;
import com.cms.cms_back.system.mapper.MediaFilesMapper;
import com.cms.cms_back.system.media.MediaCleanupProperties;
import com.cms.cms_back.system.oss.OssStorage;
import com.cms.cms_back.system.service.MediaCleanupService;

@Service
public class MediaCleanupServiceImpl implements MediaCleanupService {

    private static final Logger log = LoggerFactory.getLogger(MediaCleanupServiceImpl.class);

    private final MediaFilesMapper mediaFilesMapper;
    private final OssStorage ossStorage;
    private final MediaCleanupProperties properties;

    public MediaCleanupServiceImpl(MediaFilesMapper mediaFilesMapper,
            OssStorage ossStorage,
            MediaCleanupProperties properties) {
        this.mediaFilesMapper = mediaFilesMapper;
        this.ossStorage = ossStorage;
        this.properties = properties;
    }

    @Override
    public int cleanupStaleUploading() {
        if (!properties.isEnabled()) {
            log.info("media-cleanup disabled, skip stale uploading");
            return 0;
        }
        LocalDateTime deadline = LocalDateTime.now()
                .minusHours(properties.getStaleUploadingAfterHours());
        List<MediaFiles> list = mediaFilesMapper.selectStaleUploading(
                deadline, properties.getBatchSize());

        int success = 0;
        for (MediaFiles file : list) {
            try {
                cleanupOneStaleUploading(file);
                success++;
            } catch (Exception e) {
                log.error("清理 uploading 失败, fileId={}, objectKey={}",
                        file.getId(), file.getObjectKey(), e);
            }
        }
        log.info("cleanupStaleUploading candidates={}, success={}, dryRun={}, deadline={}",
                list.size(), success, properties.isDryRun(), deadline);
        return success;
    }

    @Override
    public int cleanupOrphanReady() {
        if (!properties.isEnabled()) {
            log.info("media-cleanup disabled, skip orphan ready");
            return 0;
        }
        LocalDateTime deadline = LocalDateTime.now()
                .minusDays(properties.getOrphanReadyAfterDays());
        List<MediaFiles> list = mediaFilesMapper.selectOrphanReadyPrivate(
                deadline, properties.getBatchSize());

        int success = 0;
        for (MediaFiles file : list) {
            try {
                cleanupOneOrphanReady(file);
                success++;
            } catch (Exception e) {
                log.error("清理孤儿 ready 失败, fileId={}, objectKey={}",
                        file.getId(), file.getObjectKey(), e);
            }
        }
        log.info("cleanupOrphanReady candidates={}, success={}, dryRun={}, deadline={}",
                list.size(), success, properties.isDryRun(), deadline);
        return success;
    }

    @Transactional(rollbackFor = Exception.class)
    protected void cleanupOneStaleUploading(MediaFiles file) {
        if (properties.isDryRun()) {
            log.info("[dry-run] stale uploading fileId={}, objectKey={}, uploadId={}",
                    file.getId(), file.getObjectKey(), file.getUploadId());
            return;
        }

        if (StringUtils.hasText(file.getUploadId())) {
            ossStorage.abortMultipart(file.getObjectKey(), file.getUploadId());
        } else {
            // 单文件可能已 put 了一部分；尽力删
            try {
                ossStorage.deleteObject(file.getObjectKey());
            } catch (Exception e) {
                log.warn("deleteObject 失败(可能未上传), fileId={}, objectKey={}",
                        file.getId(), file.getObjectKey(), e);
            }
        }

        mediaFilesMapper.update(null, new LambdaUpdateWrapper<MediaFiles>()
                .eq(MediaFiles::getId, file.getId())
                .set(MediaFiles::getStatus, MediaFilesStatus.FAILED)
                .set(MediaFiles::getUploadId, null));
    }

    @Transactional(rollbackFor = Exception.class)
    protected void cleanupOneOrphanReady(MediaFiles file) {
        if (properties.isDryRun()) {
            log.info("[dry-run] orphan ready fileId={}, objectKey={}",
                    file.getId(), file.getObjectKey());
            return;
        }

        try {
            ossStorage.deleteObject(file.getObjectKey());
        } catch (Exception e) {
            // OSS 已不存在仍落 deleted，避免反复扫
            log.warn("删 OSS 失败(可能已不存在), fileId={}, objectKey={}",
                    file.getId(), file.getObjectKey(), e);
        }

        mediaFilesMapper.update(null, new LambdaUpdateWrapper<MediaFiles>()
                .eq(MediaFiles::getId, file.getId())
                .set(MediaFiles::getStatus, MediaFilesStatus.DELETED));
    }
}
```

**注意：** Spring 同类内 `this.cleanupOneXxx` 的 `@Transactional` 可能不生效；单条失败已在外层 catch，库更新失败可接受打日志。若要严格事务，可拆成独立 Bean 或 `TransactionTemplate`。MVP 可先按上实现。

---

## 12. Job Handler（admin）

路径：`cms-back-admin/.../job/MediaCleanupJobHandler.java`

```java
package com.cms.cms_back.admin.job;

import org.springframework.stereotype.Component;

import com.cms.cms_back.system.service.MediaCleanupService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;

@Component
public class MediaCleanupJobHandler {

    private final MediaCleanupService mediaCleanupService;

    public MediaCleanupJobHandler(MediaCleanupService mediaCleanupService) {
        this.mediaCleanupService = mediaCleanupService;
    }

    /**
     * 控制台 JobHandler 必须完全一致：mediaStaleUploadingCleanupJob
     */
    @XxlJob("mediaStaleUploadingCleanupJob")
    public void staleUploadingCleanupJob() {
        XxlJobHelper.log("start mediaStaleUploadingCleanupJob");
        int count = mediaCleanupService.cleanupStaleUploading();
        XxlJobHelper.log("finished successCount=" + count);
    }

    /**
     * 控制台 JobHandler 必须完全一致：mediaOrphanReadyCleanupJob
     */
    @XxlJob("mediaOrphanReadyCleanupJob")
    public void orphanReadyCleanupJob() {
        XxlJobHelper.log("start mediaOrphanReadyCleanupJob");
        int count = mediaCleanupService.cleanupOrphanReady();
        XxlJobHelper.log("finished successCount=" + count);
    }
}
```

失败时可选：

```java
XxlJobHelper.handleFail("cleanup failed: " + e.getMessage());
```

---

## 13. XXL 控制台配置（逐步）

### 13.1 执行器

1. 登录 `http://127.0.0.1:8088/xxl-job-admin`  
2. **执行器管理** → 新增  
   - AppName：`cms-back-admin-executor`（与 yml `appname` **完全一致**）  
   - 名称：CMS Admin  
   - 注册方式：自动注册  
3. 启动 `cms-back-admin`，刷新，应看到在线机器（本机 IP:9999）

### 13.2 任务 A：超时 uploading

| 字段 | 值 |
|---|---|
| 执行器 | cms-back-admin-executor |
| 任务描述 | 媒体-uploading超时清理 |
| 调度类型 | CRON |
| Cron | `0 0 3 * * ?`（每天 03:00） |
| 运行模式 | BEAN |
| JobHandler | `mediaStaleUploadingCleanupJob` |
| 路由策略 | 第一个 |
| 阻塞处理 | 单机串行 |
| 任务超时 | 0 或 600（秒） |

### 13.3 任务 B：孤儿 ready

| 字段 | 值 |
|---|---|
| 任务描述 | 媒体-孤儿ready清理 |
| Cron | `0 30 3 * * ?`（每天 03:30） |
| JobHandler | `mediaOrphanReadyCleanupJob` |
| 其余 | 同 A |

### 13.4 先手动执行

任务列表 → **执行一次** → **调度日志** 看 `XxlJobHelper.log` 与应用日志。

---

## 14. 联调步骤（推荐）

```text
1. docker compose up -d db
2. 建 xxl_job 库 + 执行 tables_xxl_job.sql
3. 修正 compose 后 up -d xxl-job-admin
4. 浏览器打开 Admin，改密码
5. application.yml：dry-run: true
6. IDE 启动 cms-back-admin
7. 控制台确认执行器在线
8. 建两个任务，手动执行一次
9. 看日志是否列出候选 fileId（不删）
10. 造测试数据（§15），再 dry-run / 正式跑
11. dry-run: false，开 Cron
```

---

## 15. 测试用例

| # | 准备数据 | 跑哪个 Job | 预期（dry-run=false） |
|---|---|---|---|
| 1 | `uploading`，`created_at` 改为 25h 前，有 `upload_id` | A | `status=failed`，`upload_id` 清空；OSS multipart abort |
| 2 | `uploading`，无 `upload_id`，超时 | A | 尽力 `deleteObject`，`failed` |
| 3 | `ready`+`private`，无 ref，`created_at` 8 天前 | B | `deleted`，OSS 对象删除 |
| 4 | `ready`+`private`，**有** `article_media_refs` | B | 不出现在候选 |
| 5 | `ready`+`public`，无 ref | B | 不删 |
| 6 | `ready`+`private`，无 ref，刚上传 1h | B | 宽限期内不删 |
| 7 | `dry-run=true` | A/B | 只日志，库/OSS 不变 |
| 8 | `cms.media-cleanup.enabled=false` | A/B | success=0 |

造「超时」数据示例：

```sql
UPDATE media_files
SET created_at = DATE_SUB(NOW(3), INTERVAL 25 HOUR)
WHERE id = ? AND status = 'uploading';
```

---

## 16. 风险与对策

| 风险 | 对策 |
|---|---|
| 误删未保存图 | `orphan-ready-after-days ≥ 7` |
| Admin JDBC 连错 | 容器内必须 `db:3306/xxl_job` |
| 执行器注册不上 | 查 `addresses`、端口 9999 防火墙、token 是否一致 |
| 多实例重复清 | 路由「第一个」+「单机串行」 |
| OSS 删失败反复扫 | catch 后仍标 `deleted` |
| `@Transactional` 自调用 | MVP 可接受；严格则拆 Bean |
| 一次删太多 | `batch-size` + 每天跑多次自然消化 |

---

## 17. 手敲清单（打勾）

- [ ] 父 POM / admin POM：`com.xuxueli:xxl-job-core`  
- [ ] 修正 `docker-compose.yml` 的 xxl-job-admin（镜像空格、`db:3306`、`xxl_job`）  
- [ ] 建库 + `tables_xxl_job.sql`  
- [ ] `.env`：`XXL_JOB_ADMIN_*`、`XXL_JOB_ADMIN_ADDRESSES` 等  
- [ ] `application.yml`：`xxl.job.*` + `cms.media-cleanup`（先 dry-run）  
- [ ] `MediaCleanupProperties` + `XxlJobConfig`  
- [ ] `MediaFilesMapper` 方法 + XML  
- [ ] `MediaCleanupService` + Impl  
- [ ] `MediaCleanupJobHandler`  
- [ ] 控制台执行器 + 两个任务  
- [ ] dry-run 手动跑通 → 正式 Cron  

---

## 18. 与现有模块关系

- **不改** Upload / ArticleMediaRef 主流程  
- 孤儿定义 = **不在 `article_media_refs`**（与发布权限重算同一真相源）  
- `failed` / `deleted` 已在 `MediaFilesStatus`  

---

## 19. 规格自检

- [x] 字段名与 DDL / 实体一致（`object_key`、`upload_id`、`access_level`、`article_media_refs`）  
- [x] Docker Admin 连库方式写清（含错误示范）  
- [x] `.env` vs `application.yml` 职责拆开  
- [x] Properties 键名与现有 yml（`stale-uploading-after-hours` 等）对齐  
- [x] Handler 名、Cron、执行器 AppName 对照表  
- [x] dry-run / 测试用例 / 手敲清单完整  
