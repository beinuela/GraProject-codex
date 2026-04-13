package com.campus.material.modules.transfer.controller;

import com.campus.material.common.RemarkRequest;
import com.campus.material.modules.transfer.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;

class TransferControllerTest {

    @Mock
    private TransferService transferService;

    private TransferController transferController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        transferController = new TransferController(transferService);
    }

    @Test
    void approveShouldUseRequestParamRemarkWhenBodyMissing() {
        transferController.approve(11L, "param-remark", null);
        verify(transferService).approve(11L, "param-remark");
    }

    @Test
    void approveShouldPreferBodyRemarkWhenProvided() {
        RemarkRequest body = new RemarkRequest();
        body.setRemark("body-remark");

        transferController.approve(12L, "param-remark", body);
        verify(transferService).approve(12L, "body-remark");
    }

    @Test
    void rejectShouldFallbackToQueryRemarkWhenBodyBlank() {
        RemarkRequest body = new RemarkRequest();
        body.setRemark("\t");

        transferController.reject(13L, "param-remark", body);
        verify(transferService).reject(13L, "param-remark");
    }
}
