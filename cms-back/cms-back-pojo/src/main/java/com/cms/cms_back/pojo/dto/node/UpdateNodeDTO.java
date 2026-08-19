package com.cms.cms_back.pojo.dto.node;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class UpdateNodeDTO {
    @NotBlank(message = "标题不能为空")
    @Size(max = 25, message = "标题长度不能超过25个字符")
    private String title;

    @NotNull(message = "排序不能为空")
    @Min(value = 0, message = "排序不能小于0")
    private Integer sort;
}
