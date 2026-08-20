package com.cms.cms_back.pojo.dto.article;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SaveArticleDTO {

    @NotNull(message = "内容不能为空")
    private String content;

    @NotNull(message = "发布状态不能为空")
    private String publishStatus;
}
