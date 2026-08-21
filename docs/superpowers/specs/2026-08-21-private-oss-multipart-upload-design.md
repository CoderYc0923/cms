# 私有 OSS + 签名访问 + 分片直传设计

> 日期：2026-08-21  
> 状态：待评审（手敲实现）  
> 范围：文章富文本图片/视频上传、私有桶存储、签名读、大文件分片直传  
> 对齐：现有 `AdminFileController` 骨架、`RichText`（wangEditor）、`/api/admin/**` 鉴权、`/api/public/**` 公开读

---

## 1. 目标与非目标

### 目标

- 管理端富文本可上传图片、视频，写入文章 `content`。
- 对象存**私有桶**；浏览器不持有长期有效直链。
- **大文件不经应用服务器中转**，前端分片直传 OSS。
- admin / docs 都能读媒体：admin 靠登录；docs 靠「文件是否允许公开读」。
- 生产可上线：配置外置、CORS、权限、幂等、超时、可观测。

### 非目标（本期不做）

- 自动清理「上传了但未写入文章」的孤儿文件（可预留 status / 定时任务接口）。
- 图片转码、视频转码、CDN 鉴权 Cookie。
- 前端拖拽断点续传的完整 SDK 封装（文档给出流程，实现可用简易版）。

---

## 2. 关键假设（请确认）

| 项 | 假设 | 说明 |
|---|---|---|
| 云厂商 | **阿里云 OSS** | 国内「OSS」默认；接口用官方 Java SDK。本地可用同配置的测试桶。 |
| 上传路径 | **浏览器 → OSS 直传**（预签名） | 应用只签发，不接大 body。 |
| 小文件 | 小于阈值走 **单次 PUT 预签名** | 建议阈值 10MB（可配）。 |
| 大文件 | **Multipart Upload + 每分片预签名 PUT** | 分片大小建议 5～10MB。 |
| content 存什么 | **稳定业务 URL**（含 `fileId`） | 不存带过期时间的签名 URL。 |
| 公开读规则 | 文件表字段 `access_level` | 上传默认 `private`；文章**发布**时把 content 中引用的文件升为 `public`；下架可降回 `private`（见 §6）。 |

若实际是 MinIO / 腾讯云 COS / AWS S3：保持「存储抽象 + 预签名分片」不变，只换 `OssClient` 实现即可。

---

## 3. 总体架构

```text
┌──────────── admin ────────────┐     ┌──────────── docs ────────────┐
│ wangEditor customUpload       │     │ 渲染 HTML <img>/<video>      │
│  1) init / sign-parts         │     │ src = 稳定业务 URL            │
│  2) PUT 分片 → OSS            │     │ → GET /api/public/files/{id} │
│  3) complete                  │     │ → 302 → 短时签名 URL → OSS   │
│  4) insertFn(stableUrl)       │     └──────────────────────────────┘
└───────────────┬───────────────┘
                │ JWT
                ▼
┌──────────────── cms-back-admin ────────────────┐
│ AdminFileController                            │
│ FileService → OssStorage (Aliyun)              │
│ media_files 表（元数据）                        │
│ 发布时：ArticleService 扫描 content → 提权文件   │
└───────────────┬────────────────────────────────┘
                │ HTTPS API（非数据面）
                ▼
         ┌─────────────┐
         │ 私有 OSS 桶  │
         └─────────────┘
```

**原则：**

1. **控制面走后端，数据面走 OSS。**  
2. **库内 / HTML 内只存 `fileId` 对应的稳定 URL。**  
3. **真正读对象时再签 5～15 分钟 GET URL（或 302）。**

---

## 4. content 中如何存链接

推荐稳定 URL（与 API 同域，便于 CORS / Cookie / 鉴权）：

```text
/api/public/files/{fileId}/content
```

示例 HTML：

```html
<img src="/api/public/files/1001/content" alt="示意图" />
<video src="/api/public/files/1002/content" controls></video>
```

行为：

| 调用方 | 接口 | 鉴权 |
|---|---|---|
| docs / 匿名 | `GET /api/public/files/{id}/content` | 仅当 `access_level=public` 且文件 `status=ready` → **302** 到签名 URL |
| admin 预览 | `GET /api/admin/files/{id}/content` | 需登录；`private`/`public` 均可 → 302 |

> 也可用 JSON 接口返回 `{ signedUrl }`，由前端改写 DOM；**302 方案对 wangEditor / 静态 HTML 更省事**，本期推荐 302。

**禁止：** 把 `?Expires=&Signature=` 的长链写进 `articles.content`。

---

## 5. 数据模型

### 5.1 表 `media_files`（Liquibase 新变更集，如 `005-create-media-files`）

```sql
CREATE TABLE media_files (
  id            BIGINT NOT NULL AUTO_INCREMENT,
  object_key    VARCHAR(512) NOT NULL COMMENT '桶内路径',
  original_name VARCHAR(255) NULL,
  content_type  VARCHAR(128) NOT NULL,
  size_bytes    BIGINT NOT NULL DEFAULT 0,
  biz_type      VARCHAR(32) NOT NULL DEFAULT 'article_richtext' COMMENT '业务类型',
  space_id      BIGINT NULL COMMENT '可选：所属空间',
  access_level  VARCHAR(16) NOT NULL DEFAULT 'private' COMMENT 'private|public',
  status        VARCHAR(16) NOT NULL DEFAULT 'uploading' COMMENT 'uploading|ready|failed|deleted',
  upload_id     VARCHAR(128) NULL COMMENT 'OSS multipart uploadId，完成后可清空',
  etag          VARCHAR(128) NULL,
  created_by    BIGINT NULL,
  created_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_media_files_object_key (object_key),
  KEY idx_media_files_status_access (status, access_level),
  KEY idx_media_files_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='媒体文件元数据';
```

### 5.2 实体与枚举（pojo）

```java
// 示意
public enum MediaAccessLevel { PRIVATE, PUBLIC }
public enum MediaFileStatus { UPLOADING, READY, FAILED, DELETED }

@TableName("media_files")
public class MediaFile {
  private Long id;
  private String objectKey;
  private String originalName;
  private String contentType;
  private Long sizeBytes;
  private String bizType;
  private Long spaceId;
  private MediaAccessLevel accessLevel;
  private MediaFileStatus status;
  private String uploadId;
  private String etag;
  private Long createdBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
```

`articles` 表**不改**；媒体引用只活在 `content` HTML 里。

---

## 6. 发布与可见性

与现有产品语义对齐（docs 仅已发布且节点展示）：

1. **上传完成**：`access_level=private`，`status=ready`。admin 可预览；docs 访问 → 403/404。  
2. **文章发布成功后**：解析 `content` 中所有 `/api/public/files/{id}/content`，将这些 `id` 批量更新为 `public`。  
3. **保存草稿 / 下架（变 draft）**：可将文中仍引用的文件降回 `private`（推荐，避免下架后链接仍可打开）。  
4. **解析失败 / 无匹配**：不影响发布主流程，打 warn 日志。

解析示意（Service 内）：

```java
Pattern FILE_URL = Pattern.compile("/api/public/files/(\\d+)/content");

Set<Long> extractFileIds(String html) {
  if (html == null) return Set.of();
  Matcher m = FILE_URL.matcher(html);
  Set<Long> ids = new HashSet<>();
  while (m.find()) ids.add(Long.parseLong(m.group(1)));
  return ids;
}
```

挂在现有 `changeArticlePublishStatus` / publish、save-draft 成功路径末尾即可。

---

## 7. API 设计

### 7.1 管理端（需 JWT）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/admin/files/uploads/init` | 初始化：校验类型大小，建库记录，小文件返回 PUT 预签名；大文件返回 multipart `uploadId` + 建议 `partSize` |
| POST | `/api/admin/files/uploads/{fileId}/parts/sign` | 签发某一分片（或一批）的 PUT 预签名 |
| POST | `/api/admin/files/uploads/{fileId}/complete` | 完成 multipart / 确认小文件已上传，校验 Object 存在，置 `ready` |
| POST | `/api/admin/files/uploads/{fileId}/abort` | 取消分片上传，删未完成对象（可选） |
| GET | `/api/admin/files/{fileId}/content` | 登录可读 → 302 签名 GET |

> 现有 `/api/admin/files/upload` 可废弃或改为「仅小文件兼容」；推荐统一走 `uploads/*`。

#### init 请求/响应示例

```json
// POST /api/admin/files/uploads/init
{
  "fileName": "demo.mp4",
  "contentType": "video/mp4",
  "sizeBytes": 524288000,
  "bizType": "article_richtext",
  "spaceId": 1
}

// 大文件响应
{
  "fileId": 1002,
  "mode": "MULTIPART",
  "objectKey": "article/2026/08/21/{uuid}.mp4",
  "uploadId": "oss-upload-id-xxx",
  "partSize": 8388608,
  "partCount": 63,
  "stableUrl": "/api/public/files/1002/content"
}

// 小文件响应
{
  "fileId": 1001,
  "mode": "SINGLE",
  "objectKey": "article/2026/08/21/{uuid}.png",
  "putUrl": "https://bucket.oss-cn-xxx.aliyuncs.com/...?Signature=...",
  "headers": { "Content-Type": "image/png" },
  "stableUrl": "/api/public/files/1001/content"
}
```

#### 分片签名

```json
// POST /api/admin/files/uploads/1002/parts/sign
{ "partNumbers": [1, 2, 3, 4, 5] }

// 响应
{
  "parts": [
    { "partNumber": 1, "putUrl": "https://..." },
    { "partNumber": 2, "putUrl": "https://..." }
  ]
}
```

> 一次签一批（如 5～10 个），避免一次返回上百个 URL；也避免每个分片都打一枪 init 级压力。

#### complete

```json
// MULTIPART
{
  "parts": [
    { "partNumber": 1, "etag": "\"AAAA...\"" },
    { "partNumber": 2, "etag": "\"BBBB...\"" }
  ]
}

// SINGLE
{ }
```

成功后：`status=ready`，返回 `{ fileId, stableUrl, contentType, sizeBytes }`。

### 7.2 公开端（docs）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/public/files/{fileId}/content` | `ready` + `public` → 302；否则 404 |

已在 `CmsSecurityConfig` 的 `/api/public/**` permitAll，无需改安全白名单（新建 Controller 即可）。

---

## 8. objectKey 与桶配置（生产）

### 8.1 Key 规则

```text
{bizType}/{yyyy}/{MM}/{dd}/{uuid}{ext}
例：article_richtext/2026/08/21/a1b2c3d4e5.png
```

- 不要用原始文件名当 key（中文、冲突、枚举风险）。  
- `ext` 由白名单 contentType 映射，不信任客户端扩展名冒充。

### 8.2 桶与 RAM

1. 桶 ACL：**私有**（禁止公共读）。  
2. 应用使用 **RAM 子账号**，仅授权本桶：
   - `oss:PutObject` / `InitiateMultipartUpload` / `UploadPart` / `CompleteMultipartUpload` / `AbortMultipartUpload` / `GetObject` / `HeadObject` / `DeleteObject`
3. **不要**把主账号 AccessKey 打进仓库；用环境变量 / K8s Secret。  
4. 控制台为桶配置 **CORS**（前端直传必需）：

```text
来源：https://admin.example.com（及本地 http://localhost:xxxx）
方法：GET, PUT, POST, HEAD, DELETE
允许 Headers：* 或 Content-Type, Content-MD5, Authorization, x-oss-*
暴露 Headers：ETag, x-oss-request-id
```

5. 签名 GET URL 有效期建议 **300～900 秒**（可配）。  
6. 预签名 PUT / UploadPart 有效期建议 **15～60 分钟**（大文件上传要留足）。

### 8.3 应用配置（示意）

`application.yml`（敏感项用 env）：

```yaml
cms:
  oss:
    endpoint: ${OSS_ENDPOINT}           # 如 https://oss-cn-hangzhou.aliyuncs.com
    bucket: ${OSS_BUCKET}
    access-key-id: ${OSS_ACCESS_KEY_ID}
    access-key-secret: ${OSS_ACCESS_KEY_SECRET}
    # 若用自定义域名（仅签名用），可配
    # public-endpoint: https://img.example.com
    signed-get-expire-seconds: 600
    signed-put-expire-seconds: 3600
    multipart-threshold-bytes: 10485760   # 10MB
    multipart-part-size-bytes: 8388608    # 8MB
    max-image-bytes: 10485760             # 10MB
    max-video-bytes: 1073741824           # 1GB
    allowed-image-types: image/jpeg,image/png,image/gif,image/webp
    allowed-video-types: video/mp4,video/webm
```

注意：直传后 **不必**为了视频把 `spring.servlet.multipart.max-file-size` 调到 1GB；那个只影响「文件经 Spring 上传」的路径。若保留兼容小文件 multipart 进应用，可单独限制。

---

## 9. 后端模块与类职责

建议落点（贴合现有分层）：

| 层 | 位置 | 职责 |
|---|---|---|
| Controller | `cms-back-admin` …`AdminFileController` / `PublicFileController` | HTTP、参数校验 |
| Service | `cms-back-system` `FileService` / `FileServiceImpl` | 业务、DB、发布联动 |
| OSS 抽象 | `cms-back-system` 或 `framework` `OssStorage` | 预签名、multipart、head |
| 实现 | `AliyunOssStorage` | 阿里云 SDK |
| 配置 | `OssProperties` + `@ConfigurationProperties` | 绑定 `cms.oss.*` |
| 实体/DTO | `cms-back-pojo` | `MediaFile`、init/complete DTO |
| Mapper | `cms-back-system` `MediaFileMapper` | MyBatis-Plus |

依赖（父 POM 管版本，admin/system 按需引入）：

```xml
<!-- 阿里云 OSS -->
<dependency>
  <groupId>com.aliyun.oss</groupId>
  <artifactId>aliyun-sdk-oss</artifactId>
  <version>3.18.1</version><!-- 以官方最新稳定为准 -->
</dependency>
```

---

## 10. 核心流程（手敲步骤）

### 10.1 小文件（SINGLE）

```text
前端选图
  → POST init（size < threshold）
  → 拿到 putUrl
  → browser PUT file 到 putUrl（Header 带 Content-Type，与签名一致）
  → POST complete
  → insertFn(stableUrl)
  → 保存文章时 content 含 stableUrl
```

### 10.2 大文件（MULTIPART）

```text
前端选视频
  → POST init → fileId, uploadId, partSize, partCount
  → 按 partSize 切 Blob
  → 循环：parts/sign(一批) → 并行 PUT 各分片（记录返回 ETag）
  → POST complete(parts[{partNumber,etag}])
  → OSS CompleteMultipartUpload
  → status=ready，insertFn(stableUrl)
```

失败：调用 `abort`；库记录 `failed`；OSS AbortMultipartUpload。

### 10.3 读文件（302）

```text
GET /api/public/files/{id}/content
  → 查库：不存在 / 非 ready / 非 public → 404
  → generatePresignedGetUrl(objectKey, expire)
  → ResponseEntity.status(302).location(uri)
```

admin 接口跳过 `public` 校验。

---

## 11. 关键代码示意（手敲参考，非终稿）

### 11.1 存储抽象

```java
public interface OssStorage {

    /** 单次 PUT 预签名 */
    String presignPut(String objectKey, String contentType, Duration expire);

    /** 初始化分片 */
    String initiateMultipart(String objectKey, String contentType);

    /** 分片 PUT 预签名 */
    String presignUploadPart(String objectKey, String uploadId, int partNumber, Duration expire);

    /** 完成分片 */
    void completeMultipart(String objectKey, String uploadId, List<PartETag> parts);

    void abortMultipart(String objectKey, String uploadId);

    /** 读预签名 */
    String presignGet(String objectKey, Duration expire);

    /** 确认对象存在并取大小/etag */
    ObjectMeta head(String objectKey);
}
```

### 11.2 阿里云预签名 PUT（示意）

```java
@Override
public String presignPut(String objectKey, String contentType, Duration expire) {
    Date expiration = Date.from(Instant.now().plus(expire));
    GeneratePresignedUrlRequest req =
        new GeneratePresignedUrlRequest(bucket, objectKey, HttpMethod.PUT);
    req.setExpiration(expiration);
    req.setContentType(contentType); // 必须与前端 PUT 的 Content-Type 一致
    URL url = ossClient.generatePresignedUrl(req);
    return url.toString();
}
```

分片 UploadPart 预签名：同样 `HttpMethod.PUT`，并设置分片上传相关参数（以所用 SDK 版本文档为准：`UploadPartRequest` / `generatePresignedUrl` 对 partNumber、uploadId 的支持）。若 SDK 对「分片预签名」别扭，备选方案见 §14。

### 11.3 init（Service 示意）

```java
@Transactional
public UploadInitVO init(UploadInitDTO dto, Long userId) {
    validateTypeAndSize(dto); // 按 image/video 白名单与上限

    boolean multipart = dto.getSizeBytes() >= ossProperties.getMultipartThresholdBytes();
    String objectKey = buildObjectKey(dto.getBizType(), dto.getFileName(), dto.getContentType());

    MediaFile row = new MediaFile();
    row.setObjectKey(objectKey);
    row.setOriginalName(dto.getFileName());
    row.setContentType(dto.getContentType());
    row.setSizeBytes(dto.getSizeBytes());
    row.setBizType(dto.getBizType());
    row.setSpaceId(dto.getSpaceId());
    row.setAccessLevel(MediaAccessLevel.PRIVATE);
    row.setStatus(MediaFileStatus.UPLOADING);
    row.setCreatedBy(userId);
    mediaFileMapper.insert(row);

    UploadInitVO vo = new UploadInitVO();
    vo.setFileId(row.getId());
    vo.setObjectKey(objectKey);
    vo.setStableUrl("/api/public/files/" + row.getId() + "/content");

    if (!multipart) {
        vo.setMode("SINGLE");
        vo.setPutUrl(ossStorage.presignPut(objectKey, dto.getContentType(),
            Duration.ofSeconds(ossProperties.getSignedPutExpireSeconds())));
        return vo;
    }

    String uploadId = ossStorage.initiateMultipart(objectKey, dto.getContentType());
    row.setUploadId(uploadId);
    mediaFileMapper.updateById(row);

    long partSize = ossProperties.getMultipartPartSizeBytes();
    int partCount = (int) ((dto.getSizeBytes() + partSize - 1) / partSize);
    vo.setMode("MULTIPART");
    vo.setUploadId(uploadId);
    vo.setPartSize(partSize);
    vo.setPartCount(partCount);
    return vo;
}
```

### 11.4 complete（示意）

```java
@Transactional
public UploadCompleteVO complete(Long fileId, UploadCompleteDTO dto, Long userId) {
    MediaFile file = requireOwnedUploading(fileId, userId);

    if (file.getUploadId() != null) {
        ossStorage.completeMultipart(file.getObjectKey(), file.getUploadId(), toPartETags(dto.getParts()));
        file.setUploadId(null);
    } else {
        // SINGLE：HeadObject 校验已上传
        ObjectMeta meta = ossStorage.head(file.getObjectKey());
        file.setEtag(meta.getEtag());
        file.setSizeBytes(meta.getSize());
    }

    file.setStatus(MediaFileStatus.READY);
    mediaFileMapper.updateById(file);

    return UploadCompleteVO.of(file);
}
```

### 11.5 Public 302

```java
@GetMapping("/{fileId}/content")
public ResponseEntity<Void> content(@PathVariable Long fileId) {
    MediaFile file = mediaFileMapper.selectById(fileId);
    if (file == null
        || file.getStatus() != MediaFileStatus.READY
        || file.getAccessLevel() != MediaAccessLevel.PUBLIC) {
        throw BizException.notFound("file not found");
    }
    String url = ossStorage.presignGet(
        file.getObjectKey(),
        Duration.ofSeconds(ossProperties.getSignedGetExpireSeconds()));
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(url))
        .cacheControl(CacheControl.noStore())
        .build();
}
```

### 11.6 Controller 骨架替换思路

现有 `AdminFileController` 的空 `upload/get/preview` 可逐步替换为上述 `uploads/*` 与 `/{id}/content`；`PublicFileController` 新建在 `.../controllers/file/`。

---

## 12. 前端（wangEditor）手敲要点

文件：`cms-front/src/components/RichText/index.vue` 的 `customUpload`。

### 12.1 伪代码

```js
async function uploadToOss(file) {
  const init = await api.post("/api/admin/files/uploads/init", {
    fileName: file.name,
    contentType: file.type,
    sizeBytes: file.size,
    bizType: "article_richtext",
    spaceId: currentSpaceId, // 有则传
  });

  if (init.mode === "SINGLE") {
    await fetch(init.putUrl, {
      method: "PUT",
      headers: { "Content-Type": file.type },
      body: file,
    });
  } else {
    const etags = [];
    const partSize = init.partSize;
    for (let i = 0; i < init.partCount; i += batch) {
      const numbers = range(i + 1, Math.min(i + batch, init.partCount));
      const { parts } = await api.post(
        `/api/admin/files/uploads/${init.fileId}/parts/sign`,
        { partNumbers: numbers }
      );
      await Promise.all(
        parts.map(async (p) => {
          const start = (p.partNumber - 1) * partSize;
          const blob = file.slice(start, start + partSize);
          const res = await fetch(p.putUrl, {
            method: "PUT",
            body: blob,
            // Content-Type：按阿里云该预签名要求；有的分片签名不要乱加 type
          });
          const etag = res.headers.get("ETag");
          etags.push({ partNumber: p.partNumber, etag });
        })
      );
    }
    etags.sort((a, b) => a.partNumber - b.partNumber);
    await api.post(`/api/admin/files/uploads/${init.fileId}/complete`, { parts: etags });
    return init.stableUrl;
  }

  await api.post(`/api/admin/files/uploads/${init.fileId}/complete`, {});
  return init.stableUrl;
}

// wangEditor
async customUpload(file, insertFn) {
  const url = await uploadToOss(file);
  insertFn(url, file.name, url); // 图片：url, alt, href
}
```

视频：`insertFn(url, poster)`，poster 可先空。

### 12.2 注意

- 预签名 PUT 的 `Content-Type` **必须与签名时一致**，否则 OSS 会 403。  
- 浏览器读 `ETag` 依赖 OSS CORS **ExposeHeader: ETag**。  
- `stableUrl` 若是相对路径，docs/admin 不同 origin 时，应用站点把图片请求打到 **API 域名**（完整 URL：`https://api.xxx/api/public/files/...`）。生产建议 init 返回**绝对** `stableUrl`（由配置 `cms.public-api-base` 拼接）。

```yaml
cms:
  public-api-base: ${PUBLIC_API_BASE:https://api.example.com}
```

```java
vo.setStableUrl(publicApiBase + "/api/public/files/" + id + "/content");
```

---

## 13. 安全清单（上线必查）

- [ ] 桶私有；无公共读、无公共写  
- [ ] AK/SK 仅在服务端；前端只有短时预签名 URL  
- [ ] init 校验 MIME 白名单 + 大小上限（不信任扩展名）  
- [ ] complete 必须 Head/Complete 成功后才 `ready`；禁止客户端直接把任意 URL 标 ready  
- [ ] `parts/sign`、`complete`、`abort` 校验：文件属于当前用户（或管理员）且 `uploading`  
- [ ] public content：仅 `public+ready`  
- [ ] 发布/下架联动 `access_level`  
- [ ] CORS 仅放行可信前端源  
- [ ] 日志打 `fileId/objectKey/userId`，不打完整签名 URL（防日志泄露盗链）  
- [ ] XSS：docs 继续 DOMPurify；`src` 仅允许本站 `public-api` 前缀更佳（可后续收紧）

---

## 14. 备选：STS 直传（若分片预签名难搞）

若阿里云「UploadPart 预签名」在所用 SDK 版本不顺手，生产常用备选：

1. 后端用 STS AssumeRole 下发**临时 AK + Token**（权限收窄到某前缀 `article_richtext/yyyy/MM/dd/*`）。  
2. 前端用 `ali-oss` SDK：`multipartUpload(objectKey, file, { ... })`。  
3. 仍保留：先 `init` 占库拿 `fileId/objectKey`，SDK 上传成功后再 `complete`（后端 HeadObject 确认）。

STS 方案前端更省事，后端少签 N 次 part；权限模型要认真配 RAM 角色。两种都达「私有桶 + 不中转」。

**本期文档默认：预签名分片；卡住则切 STS。**

---

## 15. 推荐实现顺序（手敲里程碑）

1. **配置 + `OssProperties` + `AliyunOssStorage`**：本地用测试私有桶跑通 `presignPut` / `presignGet`。  
2. **表 `media_files` + 实体 Mapper**。  
3. **init + SINGLE put + complete + admin 302**：RichText 先只开图片。  
4. **multipart：parts/sign + complete + abort**：再开视频。  
5. **`PublicFileController` 302**。  
6. **发布/下架扫描 content 更新 `access_level`**。  
7. **生产：CORS、RAM、密钥、绝对 `stableUrl`、监控与失败 abort**。  
8. （可选）孤儿文件清理任务。

---

## 16. 测试计划

- 小图：上传 → 编辑器可见（admin content）→ 未发布时 docs 打开图 URL → 404；发布后 → 200/302 可显示。  
- 下架后：原图 public URL → 404。  
- 大视频：> threshold，分片数 > 1，中断后 abort，OSS 无残留或可生命周期规则清理碎片。  
- 伪造 complete（未实际上传）→ Head 失败 → 仍为 uploading/failed。  
- 改 Content-Type 上传 → OSS 403。  
- 无 Token 调 admin uploads → 401。  
- 签名过期后旧 GET URL 失效；稳定 URL 再请求仍可拿新签名。

---

## 17. 与现有代码的衔接点

| 现有 | 动作 |
|---|---|
| `AdminFileController` | 换成 uploads API + admin content |
| `RichText/index.vue` `customUpload` | 接 uploadToOss |
| `CmsSecurityConfig` `/api/public/**` | 已放行，加 `PublicFileController` |
| `ArticleServiceImpl` 发布/草稿 | 末尾调用 `fileService.syncAccessLevelByContent(...)` |
| `application.yml` multipart 5MB | 直传为主，可保持；勿误以为限制了 OSS 视频大小 |

---

## 18. 规格自检

- [x] 无「TBD 占位不说明」：厂商默认阿里云，备选 STS 已写清  
- [x] 与架构文档一致：admin 编辑 / docs 公开读、draft 不可见  
- [x] content 不存过期签名；私有桶 + 签名读  
- [x] 大文件分片直传，应用不中转  
- [x] 含表结构、API、配置、前后端步骤、安全与测试  

---

## 19. 待你确认的一点（可选回复）

实现前若与假设不符，只需改文档对应节：

1. OSS 是否就是**阿里云**？若是 MinIO/S3，第三节抽象不变，换 SDK。  
2. 下架后媒体是否**必须**立即变回 private？（文档默认：是）  
3. 视频上限 1GB、分片 8MB 是否合适？

确认后可按 §15 顺序手敲；需要拆「实现计划任务清单」时再说一声即可。
