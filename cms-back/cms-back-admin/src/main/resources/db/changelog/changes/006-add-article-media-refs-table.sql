CREATE TABLE IF NOT EXISTS article_media_refs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    article_id BIGINT NOT NULL COMMENT '文章ID',
    file_id BIGINT NOT NULL COMMENT '媒体文件ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_article_media_refs_article_file (article_id, file_id),
    KEY idx_article_media_refs_file_id (file_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章媒体引用表';