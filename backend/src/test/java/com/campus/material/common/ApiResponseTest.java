package com.campus.material.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiResponseTest {

    @Test
    void okShouldReturnStandardSuccessContract() {
        ApiResponse<String> response = ApiResponse.ok("data");

        assertEquals(0, response.getCode());
        assertEquals("success", response.getMessage());
        assertEquals("data", response.getData());
    }

    @Test
    void okWithMessageShouldOverrideDefaultMessage() {
        ApiResponse<Integer> response = ApiResponse.ok("created", 1);

        assertEquals(0, response.getCode());
        assertEquals("created", response.getMessage());
        assertEquals(1, response.getData());
    }

    @Test
    void failShouldReturnErrorContract() {
        ApiResponse<Void> response = ApiResponse.fail(400, "bad request");

        assertEquals(400, response.getCode());
        assertEquals("bad request", response.getMessage());
        assertNull(response.getData());
    }
}
