package com.cms.cms_back.pojo.dto.upload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InitUploadDTO {

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @NotBlank(message = "文件类型不能为空")
    private String contentType;

    @NotNull(message = "文件大小不能为空")
    private Long sizeBytes;

    @NotBlank(message = "业务类型不能为空")
    private String bizType;

    @NotNull(message = "空间ID不能为空")
    private Long spaceId;
}
