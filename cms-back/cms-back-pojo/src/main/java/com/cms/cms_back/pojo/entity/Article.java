package com.cms.cms_back.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cms.cms_back.pojo.enums.PublishStatus;

import lombok.Data;

@TableName("articles")
@Data
public class Article {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long nodeId;

    private Long spaceId;

    private String content;

    private PublishStatus publishStatus;

    private LocalDateTime publishAt;

    private Long createdBy;

    private Long updatedBy;

    private LocalDateTime deletedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
