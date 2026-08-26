package com.cms.cms_back.pojo.dto.space;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;

@Data
public class UpdateSpaceDTO {

    @NotBlank(message = "空间名称不能为空")
    @Size(max = 10, message = "空间名称长度不能超过10个字符")
    private String name;

    @Size(max = 255, message = "空间描述长度不能超过255个字符")
    private String description;

    @NotNull(message = "排序不能为空")
    @Min(value = 0, message = "排序必须大于等于0")
    private Integer sort;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
