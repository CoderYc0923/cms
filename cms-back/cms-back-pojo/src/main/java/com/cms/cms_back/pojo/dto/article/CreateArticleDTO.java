package com.cms.cms_back.pojo.dto.article;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateArticleDTO {

    @NotNull(message = "节点ID不能为空")
    private Long nodeId;

    private String content;

    private String publishStatus;
}
