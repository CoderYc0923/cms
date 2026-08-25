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
