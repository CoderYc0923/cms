package com.cms.cms_back.framework.exception;

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

import com.cms.cms_back.common.api.ApiResult;
import com.cms.cms_back.common.exception.BizException;
import com.cms.cms_back.common.exception.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理：HTTP 状态码 + 统一 body {@link ApiResult}
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** 业务异常（Service / Controller 主动 throw BizException） */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResult<Void>> handleBiz(BizException e, HttpServletRequest request) {
        log.warn("biz error, path={}, code={}, msg={}", request.getRequestURI(), e.getCode(), e.getMessage());
        return of(e.getHttpStatus(), e.getCode(), e.getMessage());
    }

    /**
     * 400：参数校验 / 绑定 / 媒体类型等问题
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
    public ResponseEntity<ApiResult<Void>> handleBadRequest(Exception e, HttpServletRequest request) {
        log.warn("bad request, path={}, ex={}", request.getRequestURI(), e.toString());
        return of(ErrorCode.BAD_REQUEST);
    }

    /**
     * 404：资源/路径不存在
     */
    @ExceptionHandler({
            HttpRequestMethodNotSupportedException.class,
            NoResourceFoundException.class,
            NoHandlerFoundException.class
    })
    public ResponseEntity<ApiResult<Void>> handleNotFound(Exception e, HttpServletRequest request) {
        log.warn("not found, path={}, ex={}", request.getRequestURI(), e.toString());
        return of(ErrorCode.NOT_FOUND);
    }

    /** 500：未预料异常 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handle(Exception e, HttpServletRequest request) {
        log.error("unhandled error, path={}", request.getRequestURI(), e);
        return of(ErrorCode.INTERNAL_ERROR);
    }

    private static ResponseEntity<ApiResult<Void>> of(ErrorCode errorCode) {
        return of(errorCode.getHttpStatus(), errorCode.getCode(), errorCode.getMessage());
    }

    private static ResponseEntity<ApiResult<Void>> of(int httpStatus, int code, String message) {
        return ResponseEntity.status(httpStatus).body(ApiResult.fail(code, message));
    }
}
