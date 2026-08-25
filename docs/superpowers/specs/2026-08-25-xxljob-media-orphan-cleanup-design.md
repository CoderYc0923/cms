# XXL-Job 媒体孤儿文件定时清理设计（手敲教程）

> 日期：2026-08-25  
> 范围：`media_files` 超时 `uploading` 清理 + 无引用 `ready` 孤儿清理  
> 执行器：挂在 `cms-back-admin`（与 Spring Boot 同进程）  
> 调度：XXL-Job Admin 控制台配置 Cron（建议凌晨低峰）

---

## 1. 目标与非目标

### 目标

| 任务 | 清理对象 | 动作 |
|---|---|---|
| **Job A** `mediaStaleUploadingCleanupJob` | `status=uploading` 且超过宽限期 | abort 分片 / 删 OSS 对象 → `failed` |
| **Job B** `mediaOrphanReadyCleanupJob` | `status=ready` + `private` + **无** `article_media_refs` + 超过宽限期 | 删 OSS → `deleted` |

### 非目标

- 不删 `public` 文件（即使暂时无 ref，避免误伤）  
- 不扫 `articles.content` 二次解析（以 refs 为准）  
- 不做物理删表行（先 `status=deleted` 软标记）

---

## 2. 为什么用 XXL-Job + 凌晨跑

- **XXL-Job**：控制台配 Cron、手动触发、看日志、失败告警，比 `@Scheduled` 适合生产。  
- **凌晨跑**：非必须，但批量删 OSS 时减轻带宽/DB 压力；**更关键**是宽限期（见 §5），避免删掉「上传完还没保存」的图。

---

## 3. 架构

```text
XXL-Job Admin（调度中心）
        │ HTTP 触发
        ▼
cms-back-admin（执行器 executor）
  ├─ MediaStaleUploadingCleanupJob  (@XxlJob)
  ├─ MediaOrphanReadyCleanupJob     (@XxlJob)
  └─ MediaCleanupService            （查库 + OSS + 改状态）
        │
        ▼
   MySQL media_files / article_media_refs
        │
        ▼
   阿里云 OSS
```

---

## 4. 依赖与模块落点

### 4.1 父 POM `cms-back/pom.xml` — `dependencyManagement`

```xml
<properties>
    <xxl-job.version>2.4.2</xxl-job.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.xuxueli</groupId>
            <artifactId>xxl-job-core</artifactId>
            <version>${xxl-job.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

> 版本以 [xxl-job releases](https://github.com/xuxueli/xxl-job) 为准；与 Spring Boot 4 一般只需 `xxl-job-core` + 自己配 Executor。

### 4.2 `cms-back-admin/pom.xml`

```xml
<dependency>
    <groupId>com.xuxueli</groupId>
    <artifactId>xxl-job-core</artifactId>
</dependency>
```

Job 类放 **admin**（执行器入口）；`MediaCleanupService` 放 **system**（复用 Mapper / OssStorage）。

---

## 5. 宽限期与默认策略（可配置）

```yaml
cms:
  media-cleanup:
  enabled: true
  batch-size: 200                    # 每批最多处理条数
  stale-uploading-after-hours: 24    # uploading 超过 24h
  orphan-ready-after-days: 7         # ready 无 ref 超过 7 天
  dry-run: false                     # true 只打日志不删 OSS/不改库
```

| 参数 | 建议 | 说明 |
|---|---|---|
| `stale-uploading-after-hours` | 24 | 用户 abandoned 上传 |
| `orphan-ready-after-days` | 7 | 防「上传完未保存」被误删 |
| `batch-size` | 200 | 防止一次删太多 |

---

## 6. XXL-Job Admin 部署（本地 / 生产）

### 6.1 Docker Compose 片段（可选，加到 `docker-compose.yml`）

```yaml
  xxl-job-admin:
    image: xuxueli/xxl-job-admin:2.4.2
    container_name: cms-xxl-job-admin
    restart: unless-stopped
    ports:
      - "8088:8080"
    environment:
      PARAMS: >-
        --spring.datasource.url=jdbc:mysql://db:3306/xxl_job?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
        --spring.datasource.username=root
        --spring.datasource.password=${MYSQL_ROOT_PASSWORD}
    depends_on:
      db:
        condition: service_healthy
```

首次需在 MySQL 建库 `xxl_job` 并执行官方 `tables_xxl_job.sql`（镜像文档 / GitHub `doc/db/tables_xxl_job.sql`）。

- 控制台默认：`http://localhost:8088/xxl-job-admin`  
- 默认账号：`admin` / `123456`（上线务必改）

### 6.2 控制台里要配的

1. **执行器管理** → AppName = `cms-back-admin-executor`（与 yml 一致）  
2. **任务管理** → 新建两个 Job（见 §12）  
3. Cron 示例：`0 0 3 * * ?`（每天 03:00）

---

## 7. 应用配置

### 7.1 `application.yml`（admin）

```yaml
xxl:
  job:
    admin:
      addresses: ${XXL_JOB_ADMIN_ADDRESSES:http://127.0.0.1:8088/xxl-job-admin}
    executor:
      appname: cms-back-admin-executor
      address:                          # 留空，自动注册 IP
      ip:
      port: ${XXL_JOB_EXECUTOR_PORT:9999}
      logpath: ${XXL_JOB_LOG_PATH:./logs/xxl-job}
      logretentiondays: 30
    accessToken: ${XXL_JOB_ACCESS_TOKEN:}   # Admin 里若配了 token，这里要一致

cms:
  media-cleanup:
    enabled: true
    batch-size: 200
    stale-uploading-after-hours: 24
    orphan-ready-after-days: 7
    dry-run: false
```

`.env` 示例：

```env
XXL_JOB_ADMIN_ADDRESSES=http://127.0.0.1:8088/xxl-job-admin
XXL_JOB_EXECUTOR_PORT=9999
XXL_JOB_ACCESS_TOKEN=
```

---

## 8. 配置类 Properties

### 8.1 `MediaCleanupProperties.java`（framework 或 system）

```java
package com.cms.cms_back.system.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "cms.media-cleanup")
public class MediaCleanupProperties {

    private boolean enabled = true;
    private int batchSize = 200;
    private int staleUploadingAfterHours = 24;
    private int orphanReadyAfterDays = 7;
    private boolean dryRun = false;
}
```

### 8.2 注册 Properties + XXL Executor

`cms-back-admin` 新建 `XxlJobConfig.java`：

```java
package com.cms.cms_back.admin.config;

import com.cms.cms_back.system.media.MediaCleanupProperties;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

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

---

## 9. Mapper 查询（XML）

### 9.1 `MediaFilesMapper.java` 增加方法

```java
List<MediaFiles> selectStaleUploading(@Param("deadline") LocalDateTime deadline,
                                      @Param("limit") int limit);

List<MediaFiles> selectOrphanReadyPrivate(@Param("deadline") LocalDateTime deadline,
                                          @Param("limit") int limit);
```

### 9.2 `MediaFilesMapper.xml`（新建 `resources/mapper/MediaFilesMapper.xml`）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.cms.cms_back.system.mapper.MediaFilesMapper">

    <select id="selectStaleUploading" resultType="com.cms.cms_back.pojo.entity.MediaFiles">
        SELECT id, object_key, upload_id, status, created_at
        FROM media_files
        WHERE status = 'uploading'
          AND created_at &lt; #{deadline}
        ORDER BY created_at ASC
        LIMIT #{limit}
    </select>

    <select id="selectOrphanReadyPrivate" resultType="com.cms.cms_back.pojo.entity.MediaFiles">
        SELECT mf.id, mf.object_key, mf.status, mf.access_level, mf.created_at
        FROM media_files mf
        WHERE mf.status = 'ready'
          AND mf.access_level = 'private'
          AND mf.created_at &lt; #{deadline}
          AND NOT EXISTS (
              SELECT 1 FROM article_media_refs r WHERE r.file_id = mf.id
          )
        ORDER BY mf.created_at ASC
        LIMIT #{limit}
    </select>

</mapper>
```

---

## 10. Service：`MediaCleanupService`

### 10.1 接口（system）

```java
package com.cms.cms_back.system.service;

public interface MediaCleanupService {

    /** 清理超时 uploading */
    int cleanupStaleUploading();

    /** 清理无引用 ready 孤儿 */
    int cleanupOrphanReady();
}
```

### 10.2 实现（完整示意）

```java
package com.cms.cms_back.system.service.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
        log.info("cleanupStaleUploading done, candidates={}, success={}, dryRun={}",
                list.size(), success, properties.isDryRun());
        return success;
    }

    @Override
    public int cleanupOrphanReady() {
        if (!properties.isEnabled()) {
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
        log.info("cleanupOrphanReady done, candidates={}, success={}, dryRun={}",
                list.size(), success, properties.isDryRun());
        return success;
    }

    @Transactional(rollbackFor = Exception.class)
    protected void cleanupOneStaleUploading(MediaFiles file) {
        if (properties.isDryRun()) {
            log.info("[dry-run] stale uploading fileId={}, objectKey={}",
                    file.getId(), file.getObjectKey());
            return;
        }
        if (StringUtils.hasText(file.getUploadId())) {
            ossStorage.abortMultipart(file.getObjectKey(), file.getUploadId());
        } else {
            ossStorage.deleteObject(file.getObjectKey());
        }
        MediaFiles update = new MediaFiles();
        update.setId(file.getId());
        update.setStatus(MediaFilesStatus.FAILED);
        update.setUploadId(null);
        mediaFilesMapper.updateById(update);
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
            // OSS 已不存在时仍可落库 deleted
            log.warn("删 OSS 失败(可能已不存在), fileId={}, objectKey={}",
                    file.getId(), file.getObjectKey(), e);
        }
        MediaFiles update = new MediaFiles();
        update.setId(file.getId());
        update.setStatus(MediaFilesStatus.DELETED);
        mediaFilesMapper.updateById(update);
    }
}
```

要点：

- **单条失败不中断整批**（Job 层 for 循环 catch）  
- **dry-run** 先上线观察  
- 与 `UploadServiceImpl.abortUpload` 逻辑一致

---

## 11. XXL-Job Handler（admin 模块）

```java
package com.cms.cms_back.admin.job;

import com.cms.cms_back.system.service.MediaCleanupService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

@Component
public class MediaCleanupJobHandler {

    private final MediaCleanupService mediaCleanupService;

    public MediaCleanupJobHandler(MediaCleanupService mediaCleanupService) {
        this.mediaCleanupService = mediaCleanupService;
    }

    /**
     * 清理超时 uploading（init 后未 complete）
     * 控制台 JobHandler 名：mediaStaleUploadingCleanupJob
     */
    @XxlJob("mediaStaleUploadingCleanupJob")
    public void staleUploadingCleanupJob() {
        XxlJobHelper.log("start mediaStaleUploadingCleanupJob");
        int count = mediaCleanupService.cleanupStaleUploading();
        XxlJobHelper.log("finished, success={}", count);
    }

    /**
     * 清理无引用 ready 孤儿文件
     * 控制台 JobHandler 名：mediaOrphanReadyCleanupJob
     */
    @XxlJob("mediaOrphanReadyCleanupJob")
    public void orphanReadyCleanupJob() {
        XxlJobHelper.log("start mediaOrphanReadyCleanupJob");
        int count = mediaCleanupService.cleanupOrphanReady();
        XxlJobHelper.log("finished, success={}", count);
    }
}
```

### `@XxlJob` 说明

| 项 | 说明 |
|---|---|
| 注解值 | 必须与 XXL 控制台 **JobHandler** 字段一致 |
| `XxlJobHelper.log` | 会写到 XXL 调度日志，控制台可看 |
| 返回值 | 简单任务可不写；失败可 `XxlJobHelper.handleFail("msg")` |

可选：支持 Job 参数覆盖 batch（高级，MVP 可跳过）：

```java
String param = XxlJobHelper.getJobParam(); // 控制台「任务参数」
```

---

## 12. XXL 控制台任务配置表

| 任务描述 | JobHandler | Cron | 路由策略 | 阻塞策略 |
|---|---|---|---|---|
| 媒体-uploading 超时清理 | `mediaStaleUploadingCleanupJob` | `0 0 3 * * ?` | 第一个 | 单机串行 |
| 媒体-孤儿 ready 清理 | `mediaOrphanReadyCleanupJob` | `0 30 3 * * ?` | 第一个 | 单机串行 |

- 两个任务错开 30 分钟，减轻 OSS 压力  
- **单机串行**：同一任务不并行跑两实例  
- 先 **手动执行一次** + `dry-run: true` 验证日志

---

## 13. 手敲顺序清单

1. 父 POM + admin 引入 `xxl-job-core`  
2. `MediaCleanupProperties` + `XxlJobConfig`  
3. `application.yml` / `.env`  
4. `MediaFilesMapper` + XML 两个查询  
5. `MediaCleanupService` + `Impl`  
6. `MediaCleanupJobHandler`（admin）  
7. 部署 XXL-Job Admin，建执行器、建两个任务  
8. `dry-run: true` 手动跑 → 看日志  
9. 改 `dry-run: false`，凌晨 Cron 生效  

---

## 14. 测试用例

| # | 场景 | 预期 |
|---|---|---|
| 1 | `uploading` 且 `created_at` 25h 前 | Job A → `failed`，OSS 无残留 |
| 2 | `ready` + private + 无 ref + 8 天前 | Job B → `deleted`，OSS 删 |
| 3 | `ready` + 有 ref | 不删 |
| 4 | `ready` + public + 无 ref | 不删（本设计） |
| 5 | 上传完 1h、未保存正文 | 7 天宽限期内不删 |
| 6 | `dry-run=true` | 只日志，库和 OSS 不变 |

---

## 15. 风险与对策

| 风险 | 对策 |
|---|---|
| 误删「未保存」图 | `orphan-ready-after-days` ≥ 7 |
| OSS 删失败 | catch 打日志；孤儿仍可标 `deleted` 避免反复扫 |
| 执行器连不上 Admin | 检查 `addresses`、token、端口 9999 |
| 多实例重复执行 | 路由「第一个」+ 阻塞「单机串行」 |
| 与业务上传并发 | 只删宽限期外 + `private` + 无 ref |

---

## 16. 与现有模块关系

- **不改** `UploadService` / `ArticleMediaRefService` 主流程  
- 孤儿定义与 **refs 表**一致，不重复解析 HTML  
- `failed` / `deleted` 状态已存在于 `MediaFilesStatus`  

---

## 17. 规格自检

- [x] XXL 依赖、配置、Executor、Handler 注解齐全  
- [x] 两条清理 SQL + Service + dry-run  
- [x] 控制台 Cron / JobHandler 命名对照  
- [x] 宽限期说明，不依赖「必须深夜」  
