package com.campus.material.common;

import io.sentry.Sentry;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBiz(BizException e) {
        log.warn("业务异常 [{}]: {}", e.getCode(), e.getMessage());
        if (e.getCode() >= ErrorCode.INTERNAL_SERVER_ERROR.getCode()) {
            Sentry.captureException(e);
        }
        return buildResponse(resolveStatus(e.getCode()), e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验异常: {}", message);
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST.getCode(), "参数验证失败: " + message);
    }

    @ExceptionHandler({
            BindException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception e) {
        String message = extractBadRequestMessage(e);
        log.warn("请求参数异常: {}", message);
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(Exception e) {
        log.warn("权限不足: {}", e.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN.getCode(), "无权限访问该资源");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException e) {
        log.warn("认证异常: {}", e.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getCode(), "登录已失效，请重新登录");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一约束冲突: {}", e.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ErrorCode.BUSINESS_ERROR.getCode(), "数据已存在，请勿重复提交");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception e) {
        log.error("服务器内部未知错误", e);
        Sentry.captureException(e);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage()
        );
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(HttpStatus status, int code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.fail(code, message));
    }

    private HttpStatus resolveStatus(int code) {
        return switch (code) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 405 -> HttpStatus.METHOD_NOT_ALLOWED;
            case 409 -> HttpStatus.CONFLICT;
            case 429 -> HttpStatus.TOO_MANY_REQUESTS;
            default -> code >= 500 ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.CONFLICT;
        };
    }

    private String extractBadRequestMessage(Exception e) {
        if (e instanceof BindException bindException) {
            String fieldMessage = bindException.getBindingResult().getFieldErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining("; "));
            if (!fieldMessage.isBlank()) {
                return "参数验证失败: " + fieldMessage;
            }
        }
        if (e instanceof ConstraintViolationException constraintViolationException) {
            String fieldMessage = constraintViolationException.getConstraintViolations().stream()
                    .map(violation -> violation.getMessage())
                    .collect(Collectors.joining("; "));
            if (!fieldMessage.isBlank()) {
                return "参数验证失败: " + fieldMessage;
            }
        }
        if (e instanceof HttpMessageNotReadableException) {
            return "请求参数格式错误";
        }
        return "请求参数错误";
    }
}
