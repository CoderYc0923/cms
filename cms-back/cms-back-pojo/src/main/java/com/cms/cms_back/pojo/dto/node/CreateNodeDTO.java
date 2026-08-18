package com.cms.cms_back.pojo.dto.node;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.cms.cms_back.pojo.enums.NodeType;

import lombok.Data;

@Data
public class CreateNodeDTO {

    @NotBlank(message = "空间标识不能为空")
    private String slug;

    private Long parentId;

    @NotNull(message = "节点类型不能为空")
    private String type;

    @NotBlank(message = "标题不能为空")
    @Size(max = 25, message = "标题长度不能超过25个字符")
    private String title;

    private Integer sort;
}
