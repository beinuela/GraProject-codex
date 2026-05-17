package com.campus.material.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "success"),
    BAD_REQUEST(400, "参数校验失败"),
    UNAUTHORIZED(401, "未认证或Token失效"),
    FORBIDDEN(403, "无权限访问该资源"),
    NOT_FOUND(404, "请求资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不被允许"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后重试"),
    BUSINESS_ERROR(409, "业务冲突"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误");

    private final int code;
    private final String defaultMessage;

    ErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}
