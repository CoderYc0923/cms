-- 001-init-schema.sql

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(128) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-正常, 0-禁用',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表';

-- 空间表
CREATE TABLE IF NOT EXISTS spaces (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    slug VARCHAR(64) NOT NULL COMMENT '唯一标识',
    description TEXT,
    sort INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-正常, 0-禁用',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_spaces_slug (slug)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '空间表';

-- 节点表
CREATE TABLE IF NOT EXISTS nodes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    space_id BIGINT NOT NULL,
    parent_id BIGINT NULL DEFAULT NULL,
    type VARCHAR(16) NOT NULL COMMENT '节点类型: group | menu | article',
    title VARCHAR(128) NOT NULL COMMENT '节点标题',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-展示, 0-隐藏',
    deleted_at DATETIME(3) NULL DEFAULT NULL COMMENT '删除时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_nodes_space_parent_sort (space_id, parent_id, sort)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '节点表';

-- 文章表
CREATE TABLE IF NOT EXISTS articles (
  id BIGINT NOT NULL AUTO_INCREMENT,
  node_id BIGINT NOT NULL,
  space_id BIGINT NOT NULL,
  content MEDIUMTEXT NULL COMMENT '文章内容',
  publish_status VARCHAR(16) NOT NULL DEFAULT 'draft' COMMENT '发布状态: draft | published',
  publish_at DATETIME(3) NULL DEFAULT NULL COMMENT '发布时间',
  created_by BIGINT NULL DEFAULT NULL COMMENT '创建者',
  updated_by BIGINT NULL DEFAULT NULL COMMENT '更新者',
  deleted_at DATETIME(3) NULL DEFAULT NULL COMMENT '删除时间',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_articles_node_id (node_id),
  KEY idx_articles_space_status (space_id, publish_status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章表';

-- 发布记录表
CREATE TABLE IF NOT EXISTS publish_events (
  id BIGINT NOT NULL AUTO_INCREMENT,
  article_id BIGINT NOT NULL,
  space_id BIGINT NOT NULL,
  event_type VARCHAR(16) NOT NULL COMMENT '事件类型: published | unpublished | updated',
  occurred_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  payload JSON NULL COMMENT '可选摘要',
  PRIMARY KEY (id),
  KEY idx_publish_events_article_occurred (article_id, occurred_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '发布记录表';