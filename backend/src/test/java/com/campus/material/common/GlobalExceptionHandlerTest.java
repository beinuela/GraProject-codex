package com.campus.material.common;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBizShouldReturnSameCodeAndMessage() {
        BizException e = new BizException(401, "unauthorized");

        ApiResponse<Void> response = handler.handleBiz(e);

        assertEquals(401, response.getCode());
        assertEquals("unauthorized", response.getMessage());
        assertEquals(null, response.getData());
    }

    @Test
    void handleMethodArgumentNotValidShouldReturnBadRequestContract() throws Exception {
        DummyRequest target = new DummyRequest();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "dummyRequest");
        bindingResult.addError(new ObjectError("dummyRequest", "field invalid"));

        Method method = DummyController.class.getDeclaredMethod("submit", DummyRequest.class);
        MethodParameter methodParameter = new MethodParameter(Objects.requireNonNull(method), 0);
        MethodArgumentNotValidException e = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ApiResponse<Void> response = handler.handleMethodArgumentNotValid(e);

        assertEquals(ErrorCode.BAD_REQUEST.getCode(), response.getCode());
        assertNotNull(response.getMessage());
    }

    @Test
    void handleBadRequestShouldReturnBadRequestContract() {
        BindException e = new BindException(new Object(), "obj");

        ApiResponse<Void> response = handler.handleBadRequest(e);

        assertEquals(ErrorCode.BAD_REQUEST.getCode(), response.getCode());
        assertNotNull(response.getMessage());
    }

    @Test
    void handleConstraintViolationShouldReturnBadRequestContract() {
        ConstraintViolationException e = new ConstraintViolationException("bad request", Collections.emptySet());

        ApiResponse<Void> response = handler.handleBadRequest(e);

        assertEquals(ErrorCode.BAD_REQUEST.getCode(), response.getCode());
        assertNotNull(response.getMessage());
    }

    @Test
    void handleHttpMessageNotReadableShouldReturnBadRequestContract() {
        HttpInputMessage inputMessage = new HttpInputMessage() {
            @Override
            @NonNull
            public java.io.InputStream getBody() {
                return new ByteArrayInputStream(new byte[0]);
            }

            @Override
            @NonNull
            public HttpHeaders getHeaders() {
                return new HttpHeaders();
            }
        };
        HttpMessageNotReadableException e = new HttpMessageNotReadableException("invalid body", inputMessage);

        ApiResponse<Void> response = handler.handleBadRequest(e);

        assertEquals(ErrorCode.BAD_REQUEST.getCode(), response.getCode());
        assertNotNull(response.getMessage());
    }

    @Test
    void handleUnknownShouldReturnInternalServerErrorContract() {
        RuntimeException e = new RuntimeException("boom");

        ApiResponse<Void> response = handler.handleUnknown(e);

        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), response.getCode());
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage(), response.getMessage());
    }

    private static class DummyController {
        @SuppressWarnings("unused")
        public void submit(DummyRequest request) {
            // no-op
        }
    }

    private static class DummyRequest {
    }
}
