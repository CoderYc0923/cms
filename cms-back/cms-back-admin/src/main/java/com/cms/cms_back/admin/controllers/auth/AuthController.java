package com.cms.cms_back.admin.controllers.auth;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.cms_back.common.api.ApiResult;
import com.cms.cms_back.framework.security.AuthService;
import com.cms.cms_back.pojo.dto.auth.LoginDTO;
import com.cms.cms_back.pojo.dto.auth.RefreshDTO;
import com.cms.cms_back.pojo.vo.auth.TokenResponseVO;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResult<TokenResponseVO> login(@Valid @RequestBody LoginDTO loginReq) {

        return ApiResult.success(authService.login(loginReq.getUsername(), loginReq.getPassword()));
    }

    @PostMapping("/refresh")
    public ApiResult<TokenResponseVO> refresh(@Valid @RequestBody RefreshDTO refreshTokenReq) {
        return ApiResult.success(authService.refresh(refreshTokenReq.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(@Valid @RequestBody RefreshDTO refreshTokenReq) {
        authService.logout(refreshTokenReq.getRefreshToken());
        return ApiResult.success();
    }

}
