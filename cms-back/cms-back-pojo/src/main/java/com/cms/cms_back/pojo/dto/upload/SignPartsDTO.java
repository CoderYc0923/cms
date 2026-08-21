package com.cms.cms_back.pojo.dto.upload;

import lombok.Data;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

@Data
public class SignPartsDTO {

    @NotEmpty(message = "分片号不能为空")
    private List<@Min(1) Integer> partNumbers;
}
