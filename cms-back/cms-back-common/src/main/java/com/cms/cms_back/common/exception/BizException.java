package com.cms.cms_back.common.exception;

import lombok.Getter;

/**
 * 业务异常
 * @author Cyrus
 * @date 2026-08-17
 * BizException
 */
@Getter
public class BizException extends RuntimeException {
    private final int httpStatus;
    private final int code;

    public BizException(int httpStatus, int code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public BizException(ErrorCode errorCode) {
        this(errorCode.getHttpStatus(), errorCode.getCode(), errorCode.getMessage());
    }

    public BizException(ErrorCode errorCode, String message) {
        this(errorCode.getHttpStatus(), errorCode.getCode(), message);
    }

    public static BizException badRequest(String message) {
        return new BizException(ErrorCode.BAD_REQUEST, message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(ErrorCode.UNAUTHORIZED, message);
    }

    public static BizException forbidden(String message) {
        return new BizException(ErrorCode.FORBIDDEN, message);
    }

    public static BizException notFound(String message) {
        return new BizException(ErrorCode.NOT_FOUND, message);
    }

    public static BizException conflict(String message) {
        return new BizException(ErrorCode.CONFLICT, message);
    }

    /**
     * 根据错误码创建业务异常
     * @param errorCode
     * @return
     */
    public static BizException of(ErrorCode errorCode) {
        return new BizException(errorCode);
    }

    /**
     * 根据错误码和消息创建业务异常
     * @param errorCode
     * @param message
     * @return
     */
    public static BizException of(ErrorCode errorCode, String message) {
        return new BizException(errorCode, message);
    }
}
