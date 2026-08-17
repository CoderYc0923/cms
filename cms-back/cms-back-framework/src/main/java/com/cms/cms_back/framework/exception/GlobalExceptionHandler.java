package com.cms.cms_back.framework.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.cms.cms_back.common.exception.BizException;
import com.cms.cms_back.common.exception.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理器
 *
 * @author Cyrus
 * @date 2026-08-17
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** 处理业务异常 */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Map<String, Object>> handleBiz(BizException e, HttpServletRequest request) {
        log.warn("biz error, path={}, code={}, msg={}", request.getRequestURI(), e.getCode(), e.getMessage());
        return build(e.getHttpStatus(), e.getCode(), e.getMessage());
    }

    /**
     * 400：各类参数/校验问题
     * 包括：
     * - 方法参数验证异常
     * - 绑定异常
     * - 约束违反异常
     * - 请求参数缺失异常
     * - 请求参数类型不匹配异常
     * - 媒体类型不支持异常
     * - 媒体类型不接受异常
     * - 路径变量缺失异常
     * - 请求头缺失异常
     * - 请求Cookie缺失异常
     * - 请求值缺失异常
     * - 请求绑定异常
     * - 方法参数验证异常
     * - 消息不可读异常
     */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class,
            MissingPathVariableException.class,
            MissingRequestHeaderException.class,
            MissingRequestCookieException.class,
            MissingRequestValueException.class,
            ServletRequestBindingException.class,
            HandlerMethodValidationException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception e, HttpServletRequest request) {
        log.warn("bad request, path={}, ex={}", request.getRequestURI(), e.toString());
        return build(
                ErrorCode.BAD_REQUEST.getHttpStatus(),
                ErrorCode.BAD_REQUEST.getCode(),
                ErrorCode.BAD_REQUEST.getMessage());
    }

    /**
     * 处理404异常
     * 包括：请求方法不支持异常、资源未找到异常、请求路径未找到异常
     * @param e
     * @param request
     * @return
     */
    @ExceptionHandler({
            HttpRequestMethodNotSupportedException.class,
            NoResourceFoundException.class,
            NoHandlerFoundException.class
    })
    public ResponseEntity<Map<String, Object>> handleNotFoundException(Exception e, HttpServletRequest request) {
        log.warn("not found, path={}, ex={}", request.getRequestURI(), e.toString());
        return build(
                ErrorCode.NOT_FOUND.getHttpStatus(),
                ErrorCode.NOT_FOUND.getCode(),
                ErrorCode.NOT_FOUND.getMessage());
    }

    /**
     * 处理未捕获的异常
     * 返回500错误
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handle(Exception e, HttpServletRequest request) {
        log.error("unhandled error, path={}", request.getRequestURI(), e);
        return build(
                ErrorCode.INTERNAL_ERROR.getHttpStatus(),
                ErrorCode.INTERNAL_ERROR.getCode(),
                ErrorCode.INTERNAL_ERROR.getMessage());
    }

    private static ResponseEntity<Map<String, Object>> build(int httpStatus, int code, String message) {
        Map<String, Object> body = new HashMap<>(4);
        body.put("code", code);
        body.put("message", message);
        body.put("data", null);
        return ResponseEntity.status(httpStatus).body(body);
    }
}
