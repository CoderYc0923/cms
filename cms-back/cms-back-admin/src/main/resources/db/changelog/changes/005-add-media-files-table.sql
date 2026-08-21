-- 添加媒体文件表
CREATE TABLE IF NOT EXISTS media_files (
    id BIGINT NOT NULL AUTO_INCREMENT,
    object_key VARCHAR(512) NOT NULL COMMENT 'OSS桶内路径',
    original_name VARCHAR(255) NULL COMMENT '原始文件名',
    content_type VARCHAR(128) NOT NULL COMMENT '文件类型',
    size_bytes BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
    biz_type VARCHAR(32) NOT NULL DEFAULT 'article_richtext' COMMENT '业务类型',
    space_id BIGINT NULL COMMENT '空间ID',
    access_level VARCHAR(16) NOT NULL DEFAULT 'private' COMMENT '访问权限：private-私有, public-公开',
    status VARCHAR(16) NOT NULL DEFAULT 'uploading' COMMENT '状态：uploading-上传中, ready-已上传, failed-上传失败, deleted-已删除',
    upload_id VARCHAR(128) NULL COMMENT 'OSS 分片上传ID， 完成后可清空',
    etag VARCHAR(128) NULL COMMENT 'OSS文件ETag',
    created_by BIGINT NULL COMMENT '创建者ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_media_files_object_key (object_key),
    KEY idx_media_files_status_access (status, access_level),
    KEY idx_media_files_created_by (created_by)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '媒体文件表';