package com.campus.emergency.modules.warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.emergency.common.WarningType;
import com.campus.emergency.modules.inventory.entity.Inventory;
import com.campus.emergency.modules.inventory.entity.InventoryBatch;
import com.campus.emergency.modules.inventory.entity.StockOutItem;
import com.campus.emergency.modules.inventory.mapper.InventoryBatchMapper;
import com.campus.emergency.modules.inventory.mapper.InventoryMapper;
import com.campus.emergency.modules.inventory.mapper.StockOutItemMapper;
import com.campus.emergency.modules.log.service.OperationLogService;
import com.campus.emergency.modules.material.entity.MaterialInfo;
import com.campus.emergency.modules.material.mapper.MaterialInfoMapper;
import com.campus.emergency.modules.warning.entity.WarningRecord;
import com.campus.emergency.modules.warning.mapper.WarningRecordMapper;
import com.campus.emergency.security.AuthUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WarningService {

    private final WarningRecordMapper warningRecordMapper;
    private final InventoryMapper inventoryMapper;
    private final MaterialInfoMapper materialInfoMapper;
    private final InventoryBatchMapper batchMapper;
    private final StockOutItemMapper stockOutItemMapper;
    private final OperationLogService operationLogService;

    public WarningService(WarningRecordMapper warningRecordMapper, InventoryMapper inventoryMapper, MaterialInfoMapper materialInfoMapper,
                          InventoryBatchMapper batchMapper, StockOutItemMapper stockOutItemMapper,
                          OperationLogService operationLogService) {
        this.warningRecordMapper = warningRecordMapper;
        this.inventoryMapper = inventoryMapper;
        this.materialInfoMapper = materialInfoMapper;
        this.batchMapper = batchMapper;
        this.stockOutItemMapper = stockOutItemMapper;
        this.operationLogService = operationLogService;
    }

    public List<WarningRecord> list(String type, String status) {
        return warningRecordMapper.selectList(new LambdaQueryWrapper<WarningRecord>()
                .eq(type != null && !type.isBlank(), WarningRecord::getWarningType, type)
                .eq(status != null && !status.isBlank(), WarningRecord::getHandleStatus, status)
                .orderByDesc(WarningRecord::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    @Scheduled(cron = "0 0/30 * * * ?")
    public void scan() {
        scanLowStock();
        scanBacklog();
        scanExpiring();
        scanExpired();
        scanAbnormalUsage();
    }

    public void handle(Long id, String remark) {
        WarningRecord warningRecord = warningRecordMapper.selectById(id);
        if (warningRecord == null) {
            return;
        }
        warningRecord.setHandleStatus("HANDLED");
        warningRecord.setHandlerId(AuthUtil.currentUserId());
        warningRecord.setHandleRemark(remark);
        warningRecordMapper.updateById(warningRecord);
        operationLogService.log(AuthUtil.currentUserId(), "WARNING", "HANDLE", "预警:" + id);
    }

    private void scanLowStock() {
        List<Inventory> inventories = inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>());
        for (Inventory inventory : inventories) {
            MaterialInfo material = materialInfoMapper.selectById(inventory.getMaterialId());
            if (material == null || material.getSafetyStock() == null) {
                continue;
            }
            if (inventory.getCurrentQty() < material.getSafetyStock()) {
                createWarningIfAbsent(WarningType.STOCK_LOW, inventory.getMaterialId(), inventory.getWarehouseId(),
                        "库存低于安全库存，当前:" + inventory.getCurrentQty() + " 安全库存:" + material.getSafetyStock());
            }
        }
    }

    private void scanBacklog() {
        List<Inventory> inventories = inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>());
        for (Inventory inventory : inventories) {
            MaterialInfo material = materialInfoMapper.selectById(inventory.getMaterialId());
            if (material == null || material.getSafetyStock() == null) {
                continue;
            }
            if (inventory.getCurrentQty() > material.getSafetyStock() * 3) {
                createWarningIfAbsent(WarningType.STOCK_BACKLOG, inventory.getMaterialId(), inventory.getWarehouseId(),
                        "库存积压风险，当前库存高于安全库存3倍");
            }
        }
    }

    private void scanExpiring() {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(30);
        List<InventoryBatch> batches = batchMapper.selectList(new LambdaQueryWrapper<InventoryBatch>()
                .gt(InventoryBatch::getRemainQty, 0)
                .ge(InventoryBatch::getExpireDate, today)
                .le(InventoryBatch::getExpireDate, threshold));
        for (InventoryBatch batch : batches) {
            createWarningIfAbsent(WarningType.EXPIRING_SOON, batch.getMaterialId(), batch.getWarehouseId(),
                    "批次临期提醒，批次:" + batch.getBatchNo() + " 过期日:" + batch.getExpireDate());
        }
    }

    private void scanExpired() {
        LocalDate today = LocalDate.now();
        List<InventoryBatch> batches = batchMapper.selectList(new LambdaQueryWrapper<InventoryBatch>()
                .gt(InventoryBatch::getRemainQty, 0)
                .lt(InventoryBatch::getExpireDate, today));
        for (InventoryBatch batch : batches) {
            createWarningIfAbsent(WarningType.EXPIRED, batch.getMaterialId(), batch.getWarehouseId(),
                    "批次已过期，批次:" + batch.getBatchNo() + " 过期日:" + batch.getExpireDate());
        }
    }

    private void scanAbnormalUsage() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStart = now.minusDays(7);
        LocalDateTime monthStart = now.minusDays(30);
        List<StockOutItem> weekItems = stockOutItemMapper.selectList(new LambdaQueryWrapper<StockOutItem>()
                .ge(StockOutItem::getCreatedAt, weekStart));
        List<StockOutItem> monthItems = stockOutItemMapper.selectList(new LambdaQueryWrapper<StockOutItem>()
                .ge(StockOutItem::getCreatedAt, monthStart));

        weekItems.stream().map(StockOutItem::getMaterialId).distinct().forEach(materialId -> {
            int weekSum = weekItems.stream().filter(it -> materialId.equals(it.getMaterialId())).mapToInt(StockOutItem::getQuantity).sum();
            int monthSum = monthItems.stream().filter(it -> materialId.equals(it.getMaterialId())).mapToInt(StockOutItem::getQuantity).sum();
            double avgWeek = monthSum / 4.0;
            if (avgWeek > 0 && weekSum > avgWeek * 1.5) {
                createWarningIfAbsent(WarningType.ABNORMAL_USAGE, materialId, null,
                        "近7日领用异常偏高，7日领用:" + weekSum + "，历史周均:" + String.format("%.2f", avgWeek));
            }
        });
    }

    private void createWarningIfAbsent(String type, Long materialId, Long warehouseId, String content) {
        WarningRecord existed = warningRecordMapper.selectOne(new LambdaQueryWrapper<WarningRecord>()
                .eq(WarningRecord::getWarningType, type)
                .eq(WarningRecord::getMaterialId, materialId)
                .eq(warehouseId != null, WarningRecord::getWarehouseId, warehouseId)
                .eq(WarningRecord::getHandleStatus, "UNHANDLED")
                .last("limit 1"));
        if (existed != null) {
            return;
        }
        WarningRecord warningRecord = new WarningRecord();
        warningRecord.setWarningType(type);
        warningRecord.setMaterialId(materialId);
        warningRecord.setWarehouseId(warehouseId);
        warningRecord.setContent(content);
        warningRecord.setHandleStatus("UNHANDLED");
        warningRecordMapper.insert(warningRecord);
    }
}
