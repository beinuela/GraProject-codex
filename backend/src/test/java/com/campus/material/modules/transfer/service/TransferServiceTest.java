package com.campus.material.modules.transfer.service;

import com.campus.material.common.BizException;
import com.campus.material.common.OrderStatus;
import com.campus.material.monitoring.BusinessMetrics;
import com.campus.material.modules.inventory.mapper.InventoryBatchMapper;
import com.campus.material.modules.inventory.mapper.InventoryMapper;
import com.campus.material.modules.log.service.OperationLogService;
import com.campus.material.modules.transfer.dto.TransferCreateRequest;
import com.campus.material.modules.transfer.entity.TransferOrder;
import com.campus.material.modules.transfer.mapper.TransferOrderItemMapper;
import com.campus.material.modules.transfer.mapper.TransferOrderMapper;
import com.campus.material.modules.warehouse.mapper.WarehouseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferServiceTest {

    @Mock
    private TransferOrderMapper transferOrderMapper;

    @Mock
    private TransferOrderItemMapper transferOrderItemMapper;

    @Mock
    private InventoryMapper inventoryMapper;

    @Mock
    private InventoryBatchMapper batchMapper;

    @Mock
    private OperationLogService operationLogService;

    @Mock
    private WarehouseMapper warehouseMapper;

    @Mock
    private BusinessMetrics businessMetrics;

    @InjectMocks
    private TransferService transferService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createShouldFailWhenFromAndToWarehouseAreSame() {
        TransferCreateRequest request = new TransferCreateRequest();
        request.setFromWarehouseId(1L);
        request.setToWarehouseId(1L);
        request.setItems(Collections.emptyList());

        BizException ex = assertThrows(BizException.class, () -> transferService.create(request));
        assertEquals(409, ex.getCode());
    }

    @Test
    void submitShouldFailWhenOrderStatusIsNotDraft() {
        TransferOrder order = new TransferOrder();
        order.setId(10L);
        order.setStatus(OrderStatus.APPROVED);
        when(transferOrderMapper.selectById(10L)).thenReturn(order);

        BizException ex = assertThrows(BizException.class, () -> transferService.submit(10L));
        assertEquals(409, ex.getCode());

        verify(transferOrderMapper, never()).updateById(order);
    }

    @Test
    void approveShouldFailWhenOrderStatusIsNotSubmitted() {
        TransferOrder order = new TransferOrder();
        order.setId(11L);
        order.setStatus(OrderStatus.DRAFT);
        when(transferOrderMapper.selectById(11L)).thenReturn(order);

        BizException ex = assertThrows(BizException.class, () -> transferService.approve(11L, "ok"));
        assertEquals(409, ex.getCode());

        verify(transferOrderMapper, never()).updateById(order);
    }

    @Test
    void detailShouldFailWhenOrderNotFound() {
        when(transferOrderMapper.selectById(404L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> transferService.detail(404L));
        assertEquals(409, ex.getCode());
    }
}
