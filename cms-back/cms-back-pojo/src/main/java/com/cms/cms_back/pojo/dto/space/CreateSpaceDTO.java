package com.cms.cms_back.pojo.dto.space;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSpaceDTO {

    @NotBlank(message = "空间名称不能为空")
    @Size(max = 10, message = "空间名称长度不能超过10个字符")
    private String name;

    @NotBlank(message = "空间slug不能为空")
    @Size(max = 10, message = "空间slug长度不能超过10个字符")
    private String slug;

    @Size(max = 255, message = "空间描述长度不能超过255个字符")
    private String description;

    @NotNull(message = "排序不能为空")
    @Min(value = 0, message = "排序必须大于等于0")
    private Integer sort;
}
