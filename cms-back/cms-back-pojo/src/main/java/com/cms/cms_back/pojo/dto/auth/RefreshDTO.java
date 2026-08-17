package com.cms.cms_back.pojo.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshDTO {

    @NotBlank(message = "refreshToken不能为空")
    private String refreshToken;
}
