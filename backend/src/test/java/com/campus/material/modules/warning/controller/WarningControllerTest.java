package com.campus.material.modules.warning.controller;

import com.campus.material.common.RemarkRequest;
import com.campus.material.modules.warning.service.WarningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;

class WarningControllerTest {

    @Mock
    private WarningService warningService;

    private WarningController warningController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        warningController = new WarningController(warningService);
    }

    @Test
    void handleShouldUseRequestParamRemarkWhenBodyMissing() {
        warningController.handle(21L, "param-remark", null);
        verify(warningService).handle(21L, "param-remark");
    }

    @Test
    void handleShouldPreferBodyRemarkWhenProvided() {
        RemarkRequest body = new RemarkRequest();
        body.setRemark("body-remark");

        warningController.handle(22L, "param-remark", body);
        verify(warningService).handle(22L, "body-remark");
    }

    @Test
    void handleShouldFallbackToQueryRemarkWhenBodyBlank() {
        RemarkRequest body = new RemarkRequest();
        body.setRemark(" ");

        warningController.handle(23L, "param-remark", body);
        verify(warningService).handle(23L, "param-remark");
    }
}
