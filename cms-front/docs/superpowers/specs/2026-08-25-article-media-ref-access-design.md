# 文章-媒体引用关系 + 权限重算设计（手敲教程）

> 日期：2026-08-25  
> 替代方案：发布时直接解析 content 改 `access_level`  
> 目标：多文章共图、发布/下架并发时权限更准确

---

## 1. 核心思路

### 1.1 两张真相

| 数据 | 含义 |
|---|---|
| `article_media_refs` | **谁引用了哪个文件**（跟正文 HTML 同步） |
| `articles.publish_status` | **文章是否已发布** |
| `media_files.access_level` | **缓存/派生字段**：是否允许 docs 公开读 |

### 1.2 公开规则（单一公式）

```text
file 应为 PUBLIC  ⟺  存在引用关系 ref，且 ref 对应的文章 publish_status = published 且未软删
```

不再在「发布接口」里直接 `SET public`，而是：

1. **保存正文** → 同步引用表（diff）  
2. **发布状态变化** → 对受影响 `file_id` **重算** `access_level`

### 1.3 和旧方案对比

| 旧 | 新 |
|---|---|
| 发布时扫 HTML 改权限 | 保存时维护 refs，权限靠重算 |
| 两篇文章共图，一篇下架会把图 private | 只要还有 published 文章引用，图保持 public |
| 正文删图不自动降权 | 保存时删 ref，再重算 → 可能变 private |

---

## 2. 数据库变更（Liquibase `006`）

### 2.1 `006-create-article-media-refs.sql`

```sql
-- 文章-媒体引用关系表
CREATE TABLE IF NOT EXISTS article_media_refs (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    article_id BIGINT NOT NULL COMMENT 'articles.id',
    file_id    BIGINT NOT NULL COMMENT 'media_files.id',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_article_media_refs_article_file (article_id, file_id),
    KEY idx_article_media_refs_file_id (file_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '文章媒体引用关系表';
```

### 2.2 `006-create-article-media-refs.yaml`

```yaml
databaseChangeLog:
  - changeSet:
      id: 006-create-article-media-refs
      author: Cyrus
      comment: 文章媒体引用关系表
      changes:
        - sqlFile:
            path: db/changelog/changes/006-create-article-media-refs.sql
            relativeToChangelogFile: false
            splitStatements: true
            stripComments: true
```

### 2.3 `db.changelog-master.yaml` 追加

```yaml
  - include:
      file: db/changelog/changes/006-create-article-media-refs.yaml
```

---

## 3. 实体与 Mapper（pojo + system）

### 3.1 `ArticleMediaRefs.java`（pojo/entity）

> 你项目里实体名是 `ArticleMediaRefs`，表名仍是 `article_media_refs`。

```java
package com.cms.cms_back.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@TableName("article_media_refs")
@Data
public class ArticleMediaRefs {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;

    private Long fileId;

    private LocalDateTime createdAt;
}
```

### 3.2 `ArticleMediaRefMapper.java`（接口，方法声明）

路径：`cms-back-system/src/main/java/.../mapper/ArticleMediaRefMapper.java`

```java
package com.cms.cms_back.system.mapper;

import java.util.Collection;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cms.cms_back.pojo.entity.ArticleMediaRefs;

@Mapper
public interface ArticleMediaRefMapper extends BaseMapper<ArticleMediaRefs> {

    /**
     * 查询某篇文章引用的所有 file_id
     */
    List<Long> selectFileIdsByArticleId(@Param("articleId") Long articleId);

    /**
     * 在候选 fileIds 中，找出「仍被至少一篇已发布文章引用」的 id
     * 调用前请保证 fileIds 非空（Service 里已判断）
     */
    List<Long> selectPublishedFileIds(@Param("fileIds") Collection<Long> fileIds);
}
```

### 3.3 `ArticleMediaRefMapper.xml`（SQL 实现）

路径：`cms-back-system/src/main/resources/mapper/ArticleMediaRefMapper.xml`

`application.yml` 已配置：

```yaml
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
```

XML 全文：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.cms.cms_back.system.mapper.ArticleMediaRefMapper">

    <!-- 某篇文章引用的 file_id 列表 -->
    <select id="selectFileIdsByArticleId" resultType="long">
        SELECT file_id
        FROM article_media_refs
        WHERE article_id = #{articleId}
    </select>

    <!--
      在候选 fileIds 中，找出仍被至少一篇「已发布且未删」文章引用的 file_id
      articles.publish_status 存的是枚举 code：published / draft
    -->
    <select id="selectPublishedFileIds" resultType="long">
        SELECT DISTINCT r.file_id
        FROM article_media_refs r
        INNER JOIN articles a ON r.article_id = a.id
        WHERE a.publish_status = 'published'
          AND a.deleted_at IS NULL
          AND r.file_id IN
        <foreach collection="fileIds" item="fileId" open="(" separator="," close=")">
            #{fileId}
        </foreach>
    </select>

</mapper>
```

手敲注意：

| 项 | 说明 |
|---|---|
| `namespace` | 必须等于 Mapper 接口全限定名 |
| `id` | 必须等于接口方法名 |
| `resultType="long"` | 单列 `file_id`，用 `long` 即可 |
| `collection="fileIds"` | 对应 `@Param("fileIds")` 参数名 |
| `item="fileId"` | 循环变量名，随意但建议语义化 |

**不要**在 `fileIds` 为空时调用 `selectPublishedFileIds`（`IN ()` 会 SQL 报错）。Service 里已有：

```java
if (fileIds == null || fileIds.isEmpty()) {
    return;
}
```

MyBatis-Plus 自带的 `insert` / `delete` 仍走 `BaseMapper`，不必写 XML。

---

## 4. 解析 content 抽 fileId（工具类）

### 4.1 `ArticleContentMediaParser.java`（system 包）

```java
package com.cms.cms_back.system.media;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ArticleContentMediaParser {

    /** 匹配 stableUrl：/api/public/files/{fileId}/content */
    private static final Pattern STABLE_URL_PATTERN =
            Pattern.compile("/api/public/files/(\\d+)/content");

    private ArticleContentMediaParser() {}

    public static Set<Long> extractFileIds(String content) {
        if (content == null || content.isBlank()) {
            return Set.of();
        }
        Matcher matcher = STABLE_URL_PATTERN.matcher(content);
        Set<Long> fileIds = new HashSet<>();
        while (matcher.find()) {
            fileIds.add(Long.parseLong(matcher.group(1)));
        }
        return fileIds;
    }
}
```

---

## 5. 核心 Service：`ArticleMediaRefService`

### 5.1 接口

```java
package com.cms.cms_back.system.service;

public interface ArticleMediaRefService {

    /**
     * 根据正文 diff 引用关系，返回本次受影响的 fileId（新增+删除+仍引用）
     */
    Set<Long> syncRefsByContent(Long articleId, Long spaceId, String content);

    /**
     * 按引用表 + 全库 published 规则，重算一批文件的 access_level
     */
    void recomputeAccessLevel(Set<Long> fileIds);

    /**
     * 重算某篇文章当前引用的所有文件
     */
    void recomputeAccessLevelForArticle(Long articleId);
}
```

### 5.2 实现（完整示意）

```java
package com.cms.cms_back.system.service.serviceImpl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cms.cms_back.common.exception.BizException;
import com.cms.cms_back.pojo.entity.ArticleMediaRefs;
import com.cms.cms_back.pojo.entity.MediaFiles;
import com.cms.cms_back.pojo.enums.MediaFilesAccessLevelType;
import com.cms.cms_back.pojo.enums.MediaFilesStatus;
import com.cms.cms_back.system.mapper.ArticleMediaRefMapper;
import com.cms.cms_back.system.mapper.MediaFilesMapper;
import com.cms.cms_back.system.media.ArticleContentMediaParser;
import com.cms.cms_back.system.service.ArticleMediaRefService;

@Service
public class ArticleMediaRefServiceImpl implements ArticleMediaRefService {

    private static final Logger log = LoggerFactory.getLogger(ArticleMediaRefServiceImpl.class);

    private final ArticleMediaRefMapper articleMediaRefMapper;
    private final MediaFilesMapper mediaFilesMapper;

    public ArticleMediaRefServiceImpl(ArticleMediaRefMapper articleMediaRefMapper,
            MediaFilesMapper mediaFilesMapper) {
        this.articleMediaRefMapper = articleMediaRefMapper;
        this.mediaFilesMapper = mediaFilesMapper;
    }

  @Override
    @Transactional(rollbackFor = Exception.class)
    public Set<Long> syncRefsByContent(Long articleId, Long spaceId, String content) {
        if (articleId == null || articleId <= 0) {
            throw BizException.badRequest("文章ID无效");
        }

        Set<Long> newFileIds = ArticleContentMediaParser.extractFileIds(content);
        validateFilesBelongToSpace(newFileIds, spaceId);

        List<Long> oldFileIdList = articleMediaRefMapper.selectFileIdsByArticleId(articleId);
        Set<Long> oldFileIds = new HashSet<>(oldFileIdList);

        Set<Long> toAdd = new HashSet<>(newFileIds);
        toAdd.removeAll(oldFileIds);

        Set<Long> toRemove = new HashSet<>(oldFileIds);
        toRemove.removeAll(newFileIds);

        for (Long fileId : toAdd) {
            ArticleMediaRefs ref = new ArticleMediaRefs();
            ref.setArticleId(articleId);
            ref.setFileId(fileId);
            articleMediaRefMapper.insert(ref);
        }

        if (!toRemove.isEmpty()) {
            articleMediaRefMapper.delete(
                    new LambdaQueryWrapper<ArticleMediaRefs>()
                            .eq(ArticleMediaRefs::getArticleId, articleId)
                            .in(ArticleMediaRefs::getFileId, toRemove));
        }

        Set<Long> affected = new HashSet<>();
        affected.addAll(oldFileIds);
        affected.addAll(newFileIds);
        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recomputeAccessLevel(Set<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }

        List<Long> shouldPublicList = articleMediaRefMapper.selectPublishedFileIds(fileIds);
        Set<Long> shouldPublic = new HashSet<>(shouldPublicList);

        Set<Long> shouldPrivate = new HashSet<>(fileIds);
        shouldPrivate.removeAll(shouldPublic);

        if (!shouldPublic.isEmpty()) {
            mediaFilesMapper.update(null,
                    new LambdaUpdateWrapper<MediaFiles>()
                            .in(MediaFiles::getId, shouldPublic)
                            .eq(MediaFiles::getStatus, MediaFilesStatus.READY)
                            .set(MediaFiles::getAccessLevel, MediaFilesAccessLevelType.PUBLIC));
        }

        if (!shouldPrivate.isEmpty()) {
            mediaFilesMapper.update(null,
                    new LambdaUpdateWrapper<MediaFiles>()
                            .in(MediaFiles::getId, shouldPrivate)
                            .eq(MediaFiles::getStatus, MediaFilesStatus.READY)
                            .set(MediaFiles::getAccessLevel, MediaFilesAccessLevelType.PRIVATE));
        }

        log.info("重算媒体访问权限完成, candidates={}, public={}, private={}",
                fileIds.size(), shouldPublic.size(), shouldPrivate.size());
    }

    @Override
    public void recomputeAccessLevelForArticle(Long articleId) {
        List<Long> fileIds = articleMediaRefMapper.selectFileIdsByArticleId(articleId);
        recomputeAccessLevel(new HashSet<>(fileIds));
    }

    /**
     * 安全：正文里引用的 file 必须存在、ready、且 space 一致（若有 space_id）
     */
    private void validateFilesBelongToSpace(Set<Long> fileIds, Long spaceId) {
        if (fileIds.isEmpty()) {
            return;
        }
        List<MediaFiles> files = mediaFilesMapper.selectBatchIds(fileIds);
        if (files.size() != fileIds.size()) {
            throw BizException.badRequest("引用了不存在的媒体文件");
        }
        for (MediaFiles file : files) {
            if (file.getStatus() != MediaFilesStatus.READY) {
                throw BizException.badRequest("媒体文件未就绪: " + file.getId());
            }
            if (spaceId != null && file.getSpaceId() != null && !spaceId.equals(file.getSpaceId())) {
                throw BizException.badRequest("媒体文件不属于当前空间: " + file.getId());
            }
        }
    }
}
```

---

## 6. 接入 `ArticleServiceImpl`（关键改动）

### 6.1 注入

```java
private final ArticleMediaRefService articleMediaRefService;

// 构造器增加参数
```

### 6.2 `save()` — 顺序很重要

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void save(Long nodeId, SaveArticleDTO dto, Long userId) {
    // ... 校验 article 存在 ...

    articleMapper.update(null, new LambdaUpdateWrapper<Article>()
            .eq(Article::getNodeId, nodeId)
            .isNull(Article::getDeletedAt)
            .set(Article::getContent, dto.getContent()));

  Article article = getArticleByNodeId(nodeId); // 拿 id / spaceId

    // 1. 同步引用（不直接改 access_level）
    Set<Long> affectedFileIds = articleMediaRefService.syncRefsByContent(
            article.getId(), article.getSpaceId(), dto.getContent());

    // 2. 改发布状态（你现有逻辑）
    changeArticlePublishStatus(nodeId, PublishStatus.formCode(dto.getPublishStatus()), userId);

    // 3. 状态已更新后，重算受影响文件权限
    articleMediaRefService.recomputeAccessLevel(affectedFileIds);
}
```

### 6.3 `changeArticlePublishStatus()` — publish / unpublish 只改状态

在 **更新 publish_status 之后**、**sendPublishEvents 之前或之后** 加：

```java
private void changeArticlePublishStatus(Long nodeId, PublishStatus publishStatus, Long userId) {
    // ... 现有校验 ...

    articleMapper.update(null, updateWrapper);

    Article article = getArticleByNodeId(nodeId);

    // 仅发布状态变化时，重算该文章引用的所有文件
    articleMediaRefService.recomputeAccessLevelForArticle(article.getId());

    sendPublishEvents(nodeId, publishStatus, isPublished, userId, article);
}
```

### 6.4 `save()` 与 `changeArticlePublishStatus` 的重算关系

- `save()` 末尾已 `recomputeAccessLevel(affectedFileIds)`  
- `changeArticlePublishStatus` 内又 `recomputeAccessLevelForArticle`  
- **会重算两次**，可接受（幂等）；想精简可让 `save()` 不调 `changeArticlePublishStatus` 里的重算，或 `save()` 只 sync refs 不重算，统一在 `changeArticlePublishStatus` 末尾重算一次。

**推荐简化版 `save()`：**

```java
articleMapper.update(...content...);
Article article = getArticleByNodeId(nodeId);
articleMediaRefService.syncRefsByContent(article.getId(), article.getSpaceId(), dto.getContent());
changeArticlePublishStatus(nodeId, PublishStatus.formCode(dto.getPublishStatus()), userId);
// changeArticlePublishStatus 内部负责 recomputeAccessLevelForArticle
```

### 6.5 `publish()` / `unpublish()`

只调 `changeArticlePublishStatus`，内部已重算，**不用改**。

### 6.6 `create()` 可选

若创建时可能带 content 和图片：

```java
articleMapper.insert(article);
if (article.getContent() != null) {
    articleMediaRefService.syncRefsByContent(article.getId(), article.getSpaceId(), article.getContent());
    articleMediaRefService.recomputeAccessLevelForArticle(article.getId());
}
```

### 6.7 `delete()` 可选

软删文章后删 refs 并重算：

```java
articleMapper.update(...deleted_at...);
List<Long> fileIds = articleMediaRefMapper.selectFileIdsByArticleId(article.getId());
articleMediaRefMapper.delete(new LambdaQueryWrapper<ArticleMediaRefs>()
        .eq(ArticleMediaRefs::getArticleId, article.getId()));
articleMediaRefService.recomputeAccessLevel(new HashSet<>(fileIds));
```

---

## 7. 完整时序（对照前端）

```text
上传 complete → stableUrl 插入编辑器（content 里仍是 URL）

保存草稿：
  update content
  → syncRefs（diff refs）
  → publish_status = draft
  → recompute：该文引用的 file 若无其他 published 引用 → private

保存并发布：
  update content
  → syncRefs
  → publish_status = published
  → recompute：引用的 file → public

仅点发布（不改正文）：
  publish_status = published
  → recompute：该文 refs 对应 file → public

取消发布：
  publish_status = draft
  → recompute：若别的 published 文仍引用 → 仍 public
```

---

## 8. 并发说明（引用方案下）

- **不需要**对 `access_level` 做 CAS  
- `syncRefs` + `recompute` 放在 `@Transactional` 里  
- 同一文章并发编辑：可对 `articles` 行 `FOR UPDATE`（可选）  
- 重算是「按当前 refs + published 集合」覆盖写，幂等  

---

## 9. 手敲顺序清单

1. Liquibase `006` 表 + master 注册  
2. `ArticleMediaRefs` 实体  
3. `ArticleMediaRefMapper` 接口 + `mapper/ArticleMediaRefMapper.xml`  
4. `ArticleContentMediaParser`  
5. `ArticleMediaRefService` + `Impl`  
6. `ArticleServiceImpl` 接入（save / changeArticlePublishStatus）  
7. 自测（见下）  

---

## 10. 测试用例

### 10.1 单文章发布/下架

1. 上传图 → complete → 插入 stableUrl  
2. 保存草稿 → `GET /api/public/files/{id}/content` → 404  
3. 发布 → public 302 可读  
4. 下架 → 404  

### 10.2 两文章共图（引用方案优势）

1. 文章 A、B 正文都引用同一 `fileId`  
2. A、B 都发布 → public  
3. 仅 B 下架 → **仍 public**（A 还在引用）  
4. A 也下架 → private  

### 10.3 正文删图

1. 发布带图 → public  
2. 保存草稿且正文删掉该图 → ref 删除 → private  

### 10.4 越权引用

正文写入其他 space 的 fileId → `syncRefs` 校验失败 → 400  

---

## 11. 与现有 Upload 模块关系

- **UploadService** 不用改；complete 仍返回 stableUrl  
- **public content 302** 仍看 `media_files.access_level = public` + `ready`  
- 权限真相在 **refs + articles.publish_status**，`access_level` 是重算结果  

---

## 12. 后续可选增强

- 定时任务：扫 `uploading` 超时、无 ref 的孤儿文件  
- `article_media_refs` 加 `space_id` 冗余加速校验  
- 发布事件 MQ 异步重算（高并发时解耦主链路）  
