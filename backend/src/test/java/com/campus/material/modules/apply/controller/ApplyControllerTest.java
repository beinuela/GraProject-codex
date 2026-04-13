package com.campus.material.modules.apply.controller;

import com.campus.material.common.RemarkRequest;
import com.campus.material.modules.apply.service.ApplyService;
import com.campus.material.modules.log.service.OperationLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;

class ApplyControllerTest {

    @Mock
    private ApplyService applyService;

    @Mock
    private OperationLogService operationLogService;

    private ApplyController applyController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        applyController = new ApplyController(applyService, operationLogService);
    }

    @Test
    void approveShouldUseRequestParamRemarkWhenBodyMissing() {
        applyController.approve(1L, "param-remark", null);
        verify(applyService).approve(1L, "param-remark");
    }

    @Test
    void approveShouldPreferBodyRemarkWhenProvided() {
        RemarkRequest body = new RemarkRequest();
        body.setRemark("body-remark");

        applyController.approve(2L, "param-remark", body);
        verify(applyService).approve(2L, "body-remark");
    }

    @Test
    void rejectShouldFallbackToQueryRemarkWhenBodyBlank() {
        RemarkRequest body = new RemarkRequest();
        body.setRemark("   ");

        applyController.reject(3L, "param-remark", body);
        verify(applyService).reject(3L, "param-remark");
    }
}
