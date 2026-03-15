package com.campus.emergency.modules.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.emergency.common.BizException;
import com.campus.emergency.common.OrderStatus;
import com.campus.emergency.common.WarningType;
import com.campus.emergency.modules.apply.entity.ApplyOrder;
import com.campus.emergency.modules.apply.entity.ApplyOrderItem;
import com.campus.emergency.modules.apply.mapper.ApplyOrderItemMapper;
import com.campus.emergency.modules.apply.mapper.ApplyOrderMapper;
import com.campus.emergency.modules.inventory.dto.InventoryAdjustRequest;
import com.campus.emergency.modules.inventory.dto.StockInRequest;
import com.campus.emergency.modules.inventory.dto.StockOutRequest;
import com.campus.emergency.modules.inventory.entity.*;
import com.campus.emergency.modules.inventory.mapper.*;
import com.campus.emergency.modules.log.service.OperationLogService;
import com.campus.emergency.modules.material.entity.MaterialInfo;
import com.campus.emergency.modules.material.mapper.MaterialInfoMapper;
import com.campus.emergency.modules.warning.entity.WarningRecord;
import com.campus.emergency.modules.warning.mapper.WarningRecordMapper;
import com.campus.emergency.security.AuthUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final InventoryMapper inventoryMapper;
    private final InventoryBatchMapper batchMapper;
    private final StockInMapper stockInMapper;
    private final StockInItemMapper stockInItemMapper;
    private final StockOutMapper stockOutMapper;
    private final StockOutItemMapper stockOutItemMapper;
    private final MaterialInfoMapper materialInfoMapper;
    private final WarningRecordMapper warningRecordMapper;
    private final ApplyOrderMapper applyOrderMapper;
    private final ApplyOrderItemMapper applyOrderItemMapper;
    private final OperationLogService operationLogService;

    public InventoryService(InventoryMapper inventoryMapper, InventoryBatchMapper batchMapper, StockInMapper stockInMapper,
                            StockInItemMapper stockInItemMapper, StockOutMapper stockOutMapper, StockOutItemMapper stockOutItemMapper,
                            MaterialInfoMapper materialInfoMapper, WarningRecordMapper warningRecordMapper,
                            ApplyOrderMapper applyOrderMapper, ApplyOrderItemMapper applyOrderItemMapper,
                            OperationLogService operationLogService) {
        this.inventoryMapper = inventoryMapper;
        this.batchMapper = batchMapper;
        this.stockInMapper = stockInMapper;
        this.stockInItemMapper = stockInItemMapper;
        this.stockOutMapper = stockOutMapper;
        this.stockOutItemMapper = stockOutItemMapper;
        this.materialInfoMapper = materialInfoMapper;
        this.warningRecordMapper = warningRecordMapper;
        this.applyOrderMapper = applyOrderMapper;
        this.applyOrderItemMapper = applyOrderItemMapper;
        this.operationLogService = operationLogService;
    }

    public List<Map<String, Object>> list(Long materialId, Long warehouseId) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<Inventory>().orderByDesc(Inventory::getId);
        if (materialId != null) {
            wrapper.eq(Inventory::getMaterialId, materialId);
        }
        if (warehouseId != null) {
            wrapper.eq(Inventory::getWarehouseId, warehouseId);
        }
        List<Inventory> inventoryList = inventoryMapper.selectList(wrapper);
        List<Long> materialIds = inventoryList.stream().map(Inventory::getMaterialId).distinct().toList();
        Map<Long, MaterialInfo> materialMap = materialIds.isEmpty()
                ? new HashMap<>()
                : materialInfoMapper.selectBatchIds(materialIds).stream().collect(Collectors.toMap(MaterialInfo::getId, m -> m));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Inventory inv : inventoryList) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", inv.getId());
            row.put("materialId", inv.getMaterialId());
            MaterialInfo info = materialMap.get(inv.getMaterialId());
            row.put("materialName", info == null ? "" : info.getMaterialName());
            row.put("safetyStock", info == null ? 0 : info.getSafetyStock());
            row.put("warehouseId", inv.getWarehouseId());
            row.put("currentQty", inv.getCurrentQty());
            row.put("lockedQty", inv.getLockedQty());
            result.add(row);
        }
        return result;
    }

    public List<InventoryBatch> batches(Long materialId, Long warehouseId) {
        return batchMapper.selectList(new LambdaQueryWrapper<InventoryBatch>()
                .eq(materialId != null, InventoryBatch::getMaterialId, materialId)
                .eq(warehouseId != null, InventoryBatch::getWarehouseId, warehouseId)
                .gt(InventoryBatch::getRemainQty, 0)
                .orderByAsc(InventoryBatch::getExpireDate)
                .orderByAsc(InventoryBatch::getId));
    }

    public List<StockIn> listStockIn() {
        return stockInMapper.selectList(new LambdaQueryWrapper<StockIn>().orderByDesc(StockIn::getId));
    }

    public List<StockOut> listStockOut() {
        return stockOutMapper.selectList(new LambdaQueryWrapper<StockOut>().orderByDesc(StockOut::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void stockIn(StockInRequest request) {
        StockIn stockIn = new StockIn();
        stockIn.setWarehouseId(request.getWarehouseId());
        stockIn.setSourceType(request.getSourceType() == null ? "PURCHASE" : request.getSourceType());
        stockIn.setOperatorId(AuthUtil.currentUserId());
        stockIn.setRemark(request.getRemark());
        stockInMapper.insert(stockIn);

        for (StockInRequest.Item item : request.getItems()) {
            StockInItem inItem = new StockInItem();
            inItem.setStockInId(stockIn.getId());
            inItem.setMaterialId(item.getMaterialId());
            inItem.setBatchNo(item.getBatchNo() == null || item.getBatchNo().isBlank()
                    ? "B" + System.currentTimeMillis() : item.getBatchNo());
            inItem.setQuantity(item.getQuantity());
            inItem.setProductionDate(item.getProductionDate());
            inItem.setExpireDate(item.getExpireDate());
            stockInItemMapper.insert(inItem);

            Inventory inventory = getOrInitInventory(item.getMaterialId(), request.getWarehouseId());
            inventory.setCurrentQty(inventory.getCurrentQty() + item.getQuantity());
            inventoryMapper.updateById(inventory);

            InventoryBatch batch = new InventoryBatch();
            batch.setMaterialId(item.getMaterialId());
            batch.setWarehouseId(request.getWarehouseId());
            batch.setBatchNo(inItem.getBatchNo());
            batch.setInQty(item.getQuantity());
            batch.setRemainQty(item.getQuantity());
            batch.setProductionDate(item.getProductionDate());
            batch.setExpireDate(item.getExpireDate());
            batchMapper.insert(batch);
        }

        operationLogService.log(AuthUtil.currentUserId(), "INVENTORY", "STOCK_IN", "入库单:" + stockIn.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void stockOut(StockOutRequest request) {
        StockOut stockOut = new StockOut();
        stockOut.setApplyOrderId(request.getApplyOrderId());
        stockOut.setWarehouseId(request.getWarehouseId());
        stockOut.setOperatorId(AuthUtil.currentUserId());
        stockOut.setRemark(request.getRemark());
        stockOutMapper.insert(stockOut);

        for (StockOutRequest.Item item : request.getItems()) {
            Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                    .eq(Inventory::getMaterialId, item.getMaterialId())
                    .eq(Inventory::getWarehouseId, request.getWarehouseId())
                    .last("limit 1"));
            if (inventory == null || inventory.getCurrentQty() < item.getQuantity()) {
                createLowStockWarning(item.getMaterialId(), request.getWarehouseId(), "出库时库存不足");
                throw new BizException("物资ID " + item.getMaterialId() + " 库存不足");
            }

            List<InventoryBatch> candidates = batchMapper.selectList(new LambdaQueryWrapper<InventoryBatch>()
                    .eq(InventoryBatch::getMaterialId, item.getMaterialId())
                    .eq(InventoryBatch::getWarehouseId, request.getWarehouseId())
                    .gt(InventoryBatch::getRemainQty, 0)
                    .ge(InventoryBatch::getExpireDate, LocalDate.now())
                    .orderByAsc(InventoryBatch::getExpireDate)
                    .orderByAsc(InventoryBatch::getId));

            int remain = item.getQuantity();
            for (InventoryBatch batch : candidates) {
                if (remain <= 0) {
                    break;
                }
                int use = Math.min(remain, batch.getRemainQty());
                batch.setRemainQty(batch.getRemainQty() - use);
                batchMapper.updateById(batch);
                remain -= use;
            }
            if (remain > 0) {
                throw new BizException("批次库存不足，无法完成出库操作");
            }

            inventory.setCurrentQty(inventory.getCurrentQty() - item.getQuantity());
            inventoryMapper.updateById(inventory);

            StockOutItem outItem = new StockOutItem();
            outItem.setStockOutId(stockOut.getId());
            outItem.setMaterialId(item.getMaterialId());
            outItem.setQuantity(item.getQuantity());
            stockOutItemMapper.insert(outItem);

            if (request.getApplyOrderId() != null) {
                ApplyOrderItem applyItem = applyOrderItemMapper.selectOne(new LambdaQueryWrapper<ApplyOrderItem>()
                        .eq(ApplyOrderItem::getApplyOrderId, request.getApplyOrderId())
                        .eq(ApplyOrderItem::getMaterialId, item.getMaterialId())
                        .last("limit 1"));
                if (applyItem != null) {
                    int old = applyItem.getActualQty() == null ? 0 : applyItem.getActualQty();
                    applyItem.setActualQty(old + item.getQuantity());
                    applyOrderItemMapper.updateById(applyItem);
                }
            }

            checkSafetyWarning(item.getMaterialId(), request.getWarehouseId());
        }

        if (request.getApplyOrderId() != null) {
            ApplyOrder applyOrder = applyOrderMapper.selectById(request.getApplyOrderId());
            if (applyOrder != null) {
                applyOrder.setStatus(OrderStatus.OUTBOUND);
                applyOrderMapper.updateById(applyOrder);
            }
        }
        operationLogService.log(AuthUtil.currentUserId(), "INVENTORY", "STOCK_OUT", "出库单:" + stockOut.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void adjust(InventoryAdjustRequest request) {
        Inventory inventory = inventoryMapper.selectById(request.getInventoryId());
        if (inventory == null) {
            throw new BizException("库存记录不存在");
        }
        inventory.setCurrentQty(request.getActualQty());
        inventoryMapper.updateById(inventory);
        operationLogService.log(AuthUtil.currentUserId(), "INVENTORY", "CHECK", "库存盘点ID:" + request.getInventoryId());
    }

    public List<InventoryBatch> recommendOutbound(Long materialId, Long warehouseId) {
        return batchMapper.selectList(new LambdaQueryWrapper<InventoryBatch>()
                .eq(InventoryBatch::getMaterialId, materialId)
                .eq(InventoryBatch::getWarehouseId, warehouseId)
                .gt(InventoryBatch::getRemainQty, 0)
                .ge(InventoryBatch::getExpireDate, LocalDate.now())
                .orderByAsc(InventoryBatch::getExpireDate)
                .orderByAsc(InventoryBatch::getId));
    }

    public Inventory getOrInitInventory(Long materialId, Long warehouseId) {
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

    private void checkSafetyWarning(Long materialId, Long warehouseId) {
        MaterialInfo material = materialInfoMapper.selectById(materialId);
        Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getMaterialId, materialId)
                .eq(Inventory::getWarehouseId, warehouseId)
                .last("limit 1"));
        if (material == null || inventory == null) {
            return;
        }
        if (material.getSafetyStock() != null && inventory.getCurrentQty() < material.getSafetyStock()) {
            createLowStockWarning(materialId, warehouseId, "库存低于安全库存阈值");
        }
    }

    private void createLowStockWarning(Long materialId, Long warehouseId, String content) {
        WarningRecord warning = new WarningRecord();
        warning.setWarningType(WarningType.STOCK_LOW);
        warning.setMaterialId(materialId);
        warning.setWarehouseId(warehouseId);
        warning.setContent(content);
        warning.setHandleStatus("UNHANDLED");
        warningRecordMapper.insert(warning);
    }
}
