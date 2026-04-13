package com.campus.material.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BizExceptionTest {

    @Test
    void constructorWithMessageShouldUseBusinessErrorCode() {
        BizException ex = new BizException("business");

        assertEquals(ErrorCode.BUSINESS_ERROR.getCode(), ex.getCode());
        assertEquals("business", ex.getMessage());
    }

    @Test
    void constructorWithCodeAndMessageShouldKeepGivenValues() {
        BizException ex = new BizException(401, "unauthorized");

        assertEquals(401, ex.getCode());
        assertEquals("unauthorized", ex.getMessage());
    }

    @Test
    void constructorWithErrorCodeShouldUseDefaultMessage() {
        BizException ex = new BizException(ErrorCode.BAD_REQUEST);

        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
        assertEquals(ErrorCode.BAD_REQUEST.getDefaultMessage(), ex.getMessage());
    }

    @Test
    void constructorWithErrorCodeAndCustomMessageShouldUseCustomMessage() {
        BizException ex = new BizException(ErrorCode.FORBIDDEN, "no permission");

        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        assertEquals("no permission", ex.getMessage());
    }
}
