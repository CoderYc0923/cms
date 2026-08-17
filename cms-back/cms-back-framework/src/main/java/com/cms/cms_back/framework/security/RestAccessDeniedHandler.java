package com.cms.cms_back.framework.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.cms.cms_back.common.api.ApiResult;
import com.cms.cms_back.common.exception.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * 访问拒绝处理器，用于处理访问拒绝的请求
 * RestAccessDeniedHandler
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 处理访问拒绝的请求
     * @param request 请求
     * @param response 响应
     * @param accessDeniedException 访问拒绝异常
     * @throws IOException 输入输出异常
     */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(ErrorCode.FORBIDDEN.getHttpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ApiResult.fail(ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getMessage()));
    }
}
