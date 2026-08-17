package com.cms.cms_back.framework.web;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 请求追踪过滤器，通过traceId追踪请求
 * @author Cyrus
 * @date 2026-08-17
 */
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_KEY = "traceId";
    private static final String PATH_KEY = "path";

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(TRACE_ID_KEY, traceId);
        MDC.put(PATH_KEY, request.getRequestURI());
        response.setHeader(TRACE_HEADER, traceId);
        try {
            // 执行下一个过滤器
            chain.doFilter(request, response);
        } finally {
            // 清除 MDC 中的 traceId 和 path，防止内存泄漏
            MDC.clear();
        }
    }
}
