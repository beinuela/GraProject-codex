package com.campus.material.modules.transfer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.material.common.BizException;
import com.campus.material.common.OrderStatus;
import com.campus.material.modules.inventory.entity.Inventory;
import com.campus.material.modules.inventory.entity.InventoryBatch;
import com.campus.material.modules.inventory.mapper.InventoryBatchMapper;
import com.campus.material.modules.inventory.mapper.InventoryMapper;
import com.campus.material.modules.log.service.OperationLogService;
import com.campus.material.modules.transfer.dto.TransferCreateRequest;
import com.campus.material.modules.transfer.entity.TransferOrder;
import com.campus.material.modules.transfer.entity.TransferOrderItem;
import com.campus.material.modules.transfer.mapper.TransferOrderItemMapper;
import com.campus.material.modules.transfer.mapper.TransferOrderMapper;
import com.campus.material.security.AuthUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
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
    private final com.campus.material.modules.warehouse.mapper.WarehouseMapper warehouseMapper;

    public TransferService(TransferOrderMapper transferOrderMapper, TransferOrderItemMapper transferOrderItemMapper,
                           InventoryMapper inventoryMapper, InventoryBatchMapper batchMapper,
                           OperationLogService operationLogService,
                           com.campus.material.modules.warehouse.mapper.WarehouseMapper warehouseMapper) {
        this.transferOrderMapper = transferOrderMapper;
        this.transferOrderItemMapper = transferOrderItemMapper;
        this.inventoryMapper = inventoryMapper;
        this.batchMapper = batchMapper;
        this.operationLogService = operationLogService;
        this.warehouseMapper = warehouseMapper;
    }

    public List<TransferOrder> list() {
        return transferOrderMapper.selectList(new LambdaQueryWrapper<TransferOrder>().orderByDesc(TransferOrder::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(TransferCreateRequest request) {
        if (request.getFromWarehouseId().equals(request.getToWarehouseId())) {
            throw new BizException("调出仓库和调入仓库不能相同");
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
        operationLogService.log(AuthUtil.currentUserId(), "TRANSFER", "CREATE", "创建调拨单:" + order.getId());
        return detail(order.getId());
    }

    public void submit(Long id) {
        TransferOrder order = mustGet(id);
        if (!OrderStatus.DRAFT.equals(order.getStatus())) {
            throw new BizException("当前状态不允许提交");
        }
        order.setStatus(OrderStatus.SUBMITTED);
        transferOrderMapper.updateById(order);
        operationLogService.log(AuthUtil.currentUserId(), "TRANSFER", "SUBMIT", "提交调拨单:" + id);
    }

    public void approve(Long id, String remark) {
        TransferOrder order = mustGet(id);
        if (!OrderStatus.SUBMITTED.equals(order.getStatus())) {
            throw new BizException("当前状态不允许审批");
        }
        order.setStatus(OrderStatus.APPROVED);
        order.setApproverId(AuthUtil.currentUserId());
        order.setApproveRemark(remark);
        order.setApproveTime(LocalDateTime.now());
        transferOrderMapper.updateById(order);
        operationLogService.log(AuthUtil.currentUserId(), "TRANSFER", "APPROVE", "审批通过调拨单:" + id);
    }

    public void reject(Long id, String remark) {
        TransferOrder order = mustGet(id);
        if (!OrderStatus.SUBMITTED.equals(order.getStatus())) {
            throw new BizException("当前状态不允许驳回");
        }
        order.setStatus(OrderStatus.REJECTED);
        order.setApproverId(AuthUtil.currentUserId());
        order.setApproveRemark(remark);
        order.setApproveTime(LocalDateTime.now());
        transferOrderMapper.updateById(order);
        operationLogService.log(AuthUtil.currentUserId(), "TRANSFER", "REJECT", "驳回调拨单:" + id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void execute(Long id) {
        TransferOrder order = mustGet(id);
        if (!OrderStatus.APPROVED.equals(order.getStatus())) {
            throw new BizException("当前状态不允许执行调拨");
        }
        List<TransferOrderItem> items = transferOrderItemMapper.selectList(
                new LambdaQueryWrapper<TransferOrderItem>().eq(TransferOrderItem::getTransferOrderId, id));

        for (TransferOrderItem item : items) {
            Inventory sourceInventory = mustGetInventory(item.getMaterialId(), order.getFromWarehouseId());
            if (sourceInventory.getCurrentQty() < item.getQuantity()) {
                throw new BizException("调拨物资库存不足，物资ID:" + item.getMaterialId());
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
                if (remain <= 0) break;
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
                throw new BizException("调拨物资批次库存不足，无法完成调拨");
            }

            sourceInventory.setCurrentQty(sourceInventory.getCurrentQty() - item.getQuantity());
            inventoryMapper.updateById(sourceInventory);

            Inventory targetInventory = getOrInitInventory(item.getMaterialId(), order.getToWarehouseId());
            targetInventory.setCurrentQty(targetInventory.getCurrentQty() + item.getQuantity());
            inventoryMapper.updateById(targetInventory);
        }

        order.setStatus(OrderStatus.OUTBOUND);
        transferOrderMapper.updateById(order);
        operationLogService.log(AuthUtil.currentUserId(), "TRANSFER", "EXECUTE", "执行调拨单:" + id);
    }

    public void receive(Long id) {
        TransferOrder order = mustGet(id);
        if (!OrderStatus.OUTBOUND.equals(order.getStatus())) {
            throw new BizException("当前状态不允许签收");
        }
        order.setStatus(OrderStatus.RECEIVED);
        transferOrderMapper.updateById(order);
        operationLogService.log(AuthUtil.currentUserId(), "TRANSFER", "RECEIVE", "签收调拨单:" + id);
    }

    public Map<String, Object> detail(Long id) {
        TransferOrder order = mustGet(id);
        List<TransferOrderItem> items = transferOrderItemMapper.selectList(
                new LambdaQueryWrapper<TransferOrderItem>().eq(TransferOrderItem::getTransferOrderId, id));
        Map<String, Object> map = new HashMap<>();
        map.put("order", order);
        map.put("items", items);
        return map;
    }

    public List<Map<String, Object>> recommendTransfer(String targetCampus, Long materialId, Integer qty) {
        // 1. 获取目标校区到所有校区的最短距离
        Map<String, Double> distances = com.campus.material.modules.algorithm.DijkstraUtil.calculateShortestPaths(targetCampus);
        
        // 2. 获取所有包含该物资且库存足够的仓库
        List<Inventory> invs = inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getMaterialId, materialId)
                .ge(Inventory::getCurrentQty, qty));
        
        List<Map<String, Object>> recommendations = new java.util.ArrayList<>();
        for (Inventory inv : invs) {
            com.campus.material.modules.warehouse.entity.Warehouse wh = warehouseMapper.selectById(inv.getWarehouseId());
            if (wh == null) continue;
            
            Double dist = distances.getOrDefault(wh.getCampus(), Double.MAX_VALUE);
            Map<String, Object> rec = new HashMap<>();
            rec.put("warehouseId", wh.getId());
            rec.put("warehouseName", wh.getWarehouseName());
            rec.put("campus", wh.getCampus());
            rec.put("distance", dist);
            rec.put("availableQty", inv.getCurrentQty());
            
            // 简单的评分权重公式：1/距离。如果在本校区，距离设为极小值。不能完全基于距离，也要看库存是否非常充裕。
            // 这里简单以距离优先升序。
            recommendations.add(rec);
        }
        
        // 根据距离从小到大排序
        recommendations.sort(Comparator.comparingDouble(m -> (Double) m.get("distance")));
        return recommendations;
    }

    private TransferOrder mustGet(Long id) {
        TransferOrder order = transferOrderMapper.selectById(id);
        if (order == null) {
            throw new BizException("调拨单不存在");
        }
        return order;
    }

    private Inventory mustGetInventory(Long materialId, Long warehouseId) {
        Inventory inv = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getMaterialId, materialId)
                .eq(Inventory::getWarehouseId, warehouseId)
                .last("limit 1"));
        if (inv == null) {
            throw new BizException("指定仓库无此物资库存记录，物资ID:" + materialId);
        }
        return inv;
    }

    private Inventory getOrInitInventory(Long materialId, Long warehouseId) {
        Inventory inv = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getMaterialId, materialId)
                .eq(Inventory::getWarehouseId, warehouseId)
                .last("limit 1"));
        if (inv != null) return inv;
        Inventory created = new Inventory();
        created.setMaterialId(materialId);
        created.setWarehouseId(warehouseId);
        created.setCurrentQty(0);
        created.setLockedQty(0);
        inventoryMapper.insert(created);
        return created;
    }
}
