package com.cms.cms_back.pojo.dto.upload;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CompleteUploadDTO {

    @Valid
    private List<CompletePartDTO> parts;

    @Data
    public static class CompletePartDTO {

        @NotNull(message = "分片号不能为空")
        @Min(value = 1, message = "分片号不能小于1")
        private Integer partNumber;

        @NotBlank(message = "ETag不能为空")
        private String etag;

    }
}
