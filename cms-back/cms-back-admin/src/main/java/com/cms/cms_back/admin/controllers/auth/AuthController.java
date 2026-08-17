package com.cms.cms_back.admin.controllers.auth;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.cms_back.common.api.ApiResult;
import com.cms.cms_back.pojo.dto.auth.LoginDTO;
import com.cms.cms_back.pojo.dto.auth.RefreshDTO;
import com.cms.cms_back.pojo.vo.auth.TokenResponseVO;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/auth")
public class AuthController {

    @PostMapping("/login")
    public ApiResult<TokenResponseVO> login(@Valid @RequestBody LoginDTO loginReq) {

        return null;
    }

    @PostMapping("/refresh")
    public ApiResult<TokenResponseVO> refresh(@Valid @RequestBody RefreshDTO refreshTokenReq) {
        return null;
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(@Valid @RequestBody RefreshDTO refreshTokenReq) {
        return null;
    }

}
