package com.campus.emergency.modules.transfer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.emergency.common.BizException;
import com.campus.emergency.common.OrderStatus;
import com.campus.emergency.modules.inventory.entity.Inventory;
import com.campus.emergency.modules.inventory.entity.InventoryBatch;
import com.campus.emergency.modules.inventory.mapper.InventoryBatchMapper;
import com.campus.emergency.modules.inventory.mapper.InventoryMapper;
import com.campus.emergency.modules.log.service.OperationLogService;
import com.campus.emergency.modules.transfer.dto.TransferCreateRequest;
import com.campus.emergency.modules.transfer.entity.TransferOrder;
import com.campus.emergency.modules.transfer.entity.TransferOrderItem;
import com.campus.emergency.modules.transfer.mapper.TransferOrderItemMapper;
import com.campus.emergency.modules.transfer.mapper.TransferOrderMapper;
import com.campus.emergency.security.AuthUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TransferService {

    private final TransferOrderMapper transferOrderMapper;
    private final TransferOrderItemMapper transferOrderItemMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryBatchMapper batchMapper;
    private final OperationLogService operationLogService;

    public TransferService(TransferOrderMapper transferOrderMapper, TransferOrderItemMapper transferOrderItemMapper,
                           InventoryMapper inventoryMapper, InventoryBatchMapper batchMapper,
                           OperationLogService operationLogService) {
        this.transferOrderMapper = transferOrderMapper;
        this.transferOrderItemMapper = transferOrderItemMapper;
        this.inventoryMapper = inventoryMapper;
        this.batchMapper = batchMapper;
        this.operationLogService = operationLogService;
    }

    public List<TransferOrder> list() {
        return transferOrderMapper.selectList(new LambdaQueryWrapper<TransferOrder>().orderByDesc(TransferOrder::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(TransferCreateRequest request) {
        if (request.getFromWarehouseId().equals(request.getToWarehouseId())) {
            throw new BizException("调出仓库与调入仓库不能相同");
        }
        TransferOrder order = new TransferOrder();
        order.setFromWarehouseId(request.getFromWarehouseId());
        order.setToWarehouseId(request.getToWarehouseId());
        order.setStatus(OrderStatus.DRAFT);
        order.setReason(request.getReason());
        order.setApplicantId(AuthUtil.currentUserId());
        transferOrderMapper.insert(order);

        for (TransferCreateRequest.Item item : request.getItems()) {
            TransferOrderItem orderItem = new TransferOrderItem();
            orderItem.setTransferOrderId(order.getId());
            orderItem.setMaterialId(item.getMaterialId());
            orderItem.setQuantity(item.getQuantity());
            transferOrderItemMapper.insert(orderItem);
        }
        operationLogService.log(AuthUtil.currentUserId(), "TRANSFER", "CREATE", "调拨单:" + order.getId());
        return detail(order.getId());
    }

    public void submit(Long id) {
        TransferOrder order = mustGet(id);
        if (!OrderStatus.DRAFT.equals(order.getStatus())) {
            throw new BizException("仅草稿状态可提交");
        }
        order.setStatus(OrderStatus.SUBMITTED);
        transferOrderMapper.updateById(order);
        operationLogService.log(AuthUtil.currentUserId(), "TRANSFER", "SUBMIT", "调拨单:" + id);
    }

    public void approve(Long id, String remark) {
        TransferOrder order = mustGet(id);
        if (!OrderStatus.SUBMITTED.equals(order.getStatus())) {
            throw new BizException("当前状态不可审批通过");
        }
        order.setStatus(OrderStatus.APPROVED);
        order.setApproverId(AuthUtil.currentUserId());
        order.setApproveRemark(remark);
        order.setApproveTime(LocalDateTime.now());
        transferOrderMapper.updateById(order);
        operationLogService.log(AuthUtil.currentUserId(), "TRANSFER", "APPROVE", "调拨单:" + id);
    }

    public void reject(Long id, String remark) {
        TransferOrder order = mustGet(id);
        if (!OrderStatus.SUBMITTED.equals(order.getStatus())) {
            throw new BizException("当前状态不可驳回");
        }
        order.setStatus(OrderStatus.REJECTED);
        order.setApproverId(AuthUtil.currentUserId());
        order.setApproveRemark(remark);
        order.setApproveTime(LocalDateTime.now());
        transferOrderMapper.updateById(order);
        operationLogService.log(AuthUtil.currentUserId(), "TRANSFER", "REJECT", "调拨单:" + id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void execute(Long id) {
        TransferOrder order = mustGet(id);
        if (!OrderStatus.APPROVED.equals(order.getStatus())) {
            throw new BizException("仅审批通过状态可执行调拨");
        }
        List<TransferOrderItem> items = transferOrderItemMapper.selectList(new LambdaQueryWrapper<TransferOrderItem>()
                .eq(TransferOrderItem::getTransferOrderId, id));

        for (TransferOrderItem item : items) {
            Inventory sourceInventory = mustGetInventory(item.getMaterialId(), order.getFromWarehouseId());
            if (sourceInventory.getCurrentQty() < item.getQuantity()) {
                throw new BizException("调拨库存不足，物资ID:" + item.getMaterialId());
            }
            List<InventoryBatch> sourceBatches = batchMapper.selectList(new LambdaQueryWrapper<InventoryBatch>()
                    .eq(InventoryBatch::getMaterialId, item.getMaterialId())
                    .eq(InventoryBatch::getWarehouseId, order.getFromWarehouseId())
                    .gt(InventoryBatch::getRemainQty, 0)
                    .ge(InventoryBatch::getExpireDate, LocalDate.now())
                    .orderByAsc(InventoryBatch::getExpireDate)
                    .orderByAsc(InventoryBatch::getId));

            int remain = item.getQuantity();
            int index = 1;
            for (InventoryBatch sourceBatch : sourceBatches) {
                if (remain <= 0) {
                    break;
                }
                int transferQty = Math.min(remain, sourceBatch.getRemainQty());
                sourceBatch.setRemainQty(sourceBatch.getRemainQty() - transferQty);
                batchMapper.updateById(sourceBatch);

                InventoryBatch targetBatch = new InventoryBatch();
                targetBatch.setMaterialId(item.getMaterialId());
                targetBatch.setWarehouseId(order.getToWarehouseId());
                targetBatch.setBatchNo(sourceBatch.getBatchNo() + "-T" + id + "-" + index++);
                targetBatch.setInQty(transferQty);
                targetBatch.setRemainQty(transferQty);
                targetBatch.setProductionDate(sourceBatch.getProductionDate());
                targetBatch.setExpireDate(sourceBatch.getExpireDate());
                batchMapper.insert(targetBatch);

                remain -= transferQty;
            }
            if (remain > 0) {
                throw new BizException("调拨失败，来源批次可用量不足");
            }

            sourceInventory.setCurrentQty(sourceInventory.getCurrentQty() - item.getQuantity());
            inventoryMapper.updateById(sourceInventory);

            Inventory targetInventory = getOrInitInventory(item.getMaterialId(), order.getToWarehouseId());
            targetInventory.setCurrentQty(targetInventory.getCurrentQty() + item.getQuantity());
            inventoryMapper.updateById(targetInventory);
        }

        order.setStatus(OrderStatus.OUTBOUND);
        transferOrderMapper.updateById(order);
        operationLogService.log(AuthUtil.currentUserId(), "TRANSFER", "EXECUTE", "调拨单:" + id);
    }

    public void receive(Long id) {
        TransferOrder order = mustGet(id);
        if (!OrderStatus.OUTBOUND.equals(order.getStatus())) {
            throw new BizException("仅已调出状态可确认调入");
        }
        order.setStatus(OrderStatus.RECEIVED);
        transferOrderMapper.updateById(order);
        operationLogService.log(AuthUtil.currentUserId(), "TRANSFER", "RECEIVE", "调拨单:" + id);
    }

    public Map<String, Object> detail(Long id) {
        TransferOrder order = mustGet(id);
        List<TransferOrderItem> items = transferOrderItemMapper.selectList(new LambdaQueryWrapper<TransferOrderItem>()
                .eq(TransferOrderItem::getTransferOrderId, id));
        Map<String, Object> map = new HashMap<>();
        map.put("order", order);
        map.put("items", items);
        return map;
    }

    private TransferOrder mustGet(Long id) {
        TransferOrder order = transferOrderMapper.selectById(id);
        if (order == null) {
            throw new BizException("调拨单不存在");
        }
        return order;
    }

    private Inventory mustGetInventory(Long materialId, Long warehouseId) {
        Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getMaterialId, materialId)
                .eq(Inventory::getWarehouseId, warehouseId)
                .last("limit 1"));
        if (inventory == null) {
            throw new BizException("无可用库存，物资ID:" + materialId);
        }
        return inventory;
    }

    private Inventory getOrInitInventory(Long materialId, Long warehouseId) {
        Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getMaterialId, materialId)
                .eq(Inventory::getWarehouseId, warehouseId)
                .last("limit 1"));
        if (inventory != null) {
            return inventory;
        }
        Inventory created = new Inventory();
        created.setMaterialId(materialId);
        created.setWarehouseId(warehouseId);
        created.setCurrentQty(0);
        created.setLockedQty(0);
        inventoryMapper.insert(created);
        return created;
    }
}
