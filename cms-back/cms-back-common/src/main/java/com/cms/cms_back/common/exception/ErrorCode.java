package com.cms.cms_back.common.exception;

import lombok.Getter;

/**
 * 错误码
 * @author Cyrus
 * @date 2026-08-17
 * ErrorCode
 */
@Getter
public enum ErrorCode {

    BAD_REQUEST(400, 400, "请求参数错误"),
    UNAUTHORIZED(401, 401, "未登录或登录已失效"),
    FORBIDDEN(403, 403, "无权限访问"),
    NOT_FOUND(404, 404, "资源不存在"),
    CONFLICT(409, 409, "资源冲突"),
    INTERNAL_ERROR(500, 500, "内部服务器错误");

    private final int httpStatus;
    private final int code;
    private final String message;

    ErrorCode(int httpStatus, int code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
