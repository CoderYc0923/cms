package com.cms.cms_back.pojo.dto.article;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SaveArticleDTO {

    @NotBlank(message = "内容不能为空")
    private String content;

    @NotBlank(message = "发布状态不能为空")
    private String publishStatus;
}
