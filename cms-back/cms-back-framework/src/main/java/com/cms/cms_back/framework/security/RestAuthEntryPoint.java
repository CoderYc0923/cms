package com.cms.cms_back.framework.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.cms.cms_back.common.api.ApiResult;
import com.cms.cms_back.common.exception.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * 认证入口点，用于处理未认证的请求
 * RestAuthEntryPoint
 */
@Component
public class RestAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 处理未认证的请求
     * @param request 请求
     * @param response 响应
     * @param authException 认证异常
     * @throws IOException 输入输出异常
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        write(response, ErrorCode.UNAUTHORIZED);
    }

    /**
     * 写入响应
     * @param response 响应
     * @param errorCode 错误码
     * @throws IOException 输入输出异常
     */
    private void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ApiResult.fail(errorCode.getCode(), errorCode.getMessage()));
    }
}
