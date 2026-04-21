package com.campus.material.modules.apply.service;

import com.campus.material.common.BizException;
import com.campus.material.monitoring.BusinessMetrics;
import com.campus.material.modules.apply.dto.ApplyCreateRequest;
import com.campus.material.modules.apply.entity.ApplyOrderItem;
import com.campus.material.modules.apply.entity.ApplyOrder;
import com.campus.material.modules.apply.mapper.ApplyOrderItemMapper;
import com.campus.material.modules.apply.mapper.ApplyOrderMapper;
import com.campus.material.modules.log.service.OperationLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ApplyServiceTest {

    @Mock
    private ApplyOrderMapper applyOrderMapper;

    @Mock
    private ApplyOrderItemMapper applyOrderItemMapper;

    @Mock
    private OperationLogService operationLogService;

    @Mock
    private BusinessMetrics businessMetrics;

    @InjectMocks
    private ApplyService applyService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateApplyOrder() {
        ApplyCreateRequest request = new ApplyCreateRequest();
        request.setDeptId(1L);
        request.setReason("测试申领");
        request.setUrgencyLevel(1);
        
        List<ApplyCreateRequest.Item> items = new ArrayList<>();
        ApplyCreateRequest.Item item1 = new ApplyCreateRequest.Item();
        item1.setMaterialId(101L);
        item1.setApplyQty(5);
        items.add(item1);
        request.setItems(items);

        ApplyOrder mockOrder = new ApplyOrder();
        mockOrder.setId(100L);

        doAnswer(invocation -> {
            ApplyOrder arg = invocation.getArgument(0);
            arg.setId(100L);
            return 1;
        }).when(applyOrderMapper).insert(any(ApplyOrder.class));

        when(applyOrderMapper.selectById(100L)).thenReturn(mockOrder);

        Map<String, Object> result = applyService.create(request);

        assertNotNull(result);
        verify(applyOrderMapper, times(1)).insert(any(ApplyOrder.class));
        verify(applyOrderItemMapper, times(1)).insert(any(ApplyOrderItem.class));
    }

    @Test
    void testApproveValidOrder() {
        ApplyOrder mockOrder = new ApplyOrder();
        mockOrder.setId(10L);
        mockOrder.setStatus("SUBMITTED");

        when(applyOrderMapper.selectById(10L)).thenReturn(mockOrder);

        applyService.approve(10L, "同意申请");

        assertEquals("APPROVED", mockOrder.getStatus());
        verify(applyOrderMapper, times(1)).updateById(mockOrder);
    }

    @Test
    void testApproveInvalidStatus() {
        ApplyOrder mockOrder = new ApplyOrder();
        mockOrder.setId(10L);
        mockOrder.setStatus("DRAFT"); // Only SUBMITTED can be approved

        when(applyOrderMapper.selectById(10L)).thenReturn(mockOrder);

        BizException thrown = assertThrows(BizException.class, () -> applyService.approve(10L, "同意申请"));
        assertTrue(thrown.getMessage().contains("不允许审批"));
    }
}
