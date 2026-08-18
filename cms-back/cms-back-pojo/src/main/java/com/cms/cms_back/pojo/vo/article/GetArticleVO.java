package com.cms.cms_back.pojo.vo.article;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GetArticleVO {

    private Long id;

    private Long nodeId;

    private String content;

    private String publishStatus;

    private LocalDateTime publishAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long createdBy;

    private Long updatedBy;
}
