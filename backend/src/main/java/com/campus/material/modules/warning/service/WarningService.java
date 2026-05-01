package com.campus.material.modules.warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.material.common.PageQuery;
import com.campus.material.common.PageResult;
import com.campus.material.common.WarningType;
import com.campus.material.monitoring.BusinessMetrics;
import com.campus.material.modules.inventory.entity.Inventory;
import com.campus.material.modules.inventory.entity.InventoryBatch;
import com.campus.material.modules.inventory.entity.StockOutItem;
import com.campus.material.modules.inventory.mapper.InventoryBatchMapper;
import com.campus.material.modules.inventory.mapper.InventoryMapper;
import com.campus.material.modules.inventory.mapper.StockOutItemMapper;
import com.campus.material.modules.log.service.OperationLogService;
import com.campus.material.modules.material.entity.MaterialInfo;
import com.campus.material.modules.material.mapper.MaterialInfoMapper;
import com.campus.material.modules.warning.entity.WarningRecord;
import com.campus.material.modules.warning.mapper.WarningRecordMapper;
import com.campus.material.security.AuthUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WarningService {

    private final WarningRecordMapper warningRecordMapper;
    private final InventoryMapper inventoryMapper;
    private final MaterialInfoMapper materialInfoMapper;
    private final InventoryBatchMapper batchMapper;
    private final StockOutItemMapper stockOutItemMapper;
    private final OperationLogService operationLogService;
    private final BusinessMetrics businessMetrics;

    public WarningService(WarningRecordMapper warningRecordMapper, InventoryMapper inventoryMapper, MaterialInfoMapper materialInfoMapper,
                          InventoryBatchMapper batchMapper, StockOutItemMapper stockOutItemMapper,
                          OperationLogService operationLogService, BusinessMetrics businessMetrics) {
        this.warningRecordMapper = warningRecordMapper;
        this.inventoryMapper = inventoryMapper;
        this.materialInfoMapper = materialInfoMapper;
        this.batchMapper = batchMapper;
        this.stockOutItemMapper = stockOutItemMapper;
        this.operationLogService = operationLogService;
        this.businessMetrics = businessMetrics;
    }

    public PageResult<WarningRecord> list(PageQuery pageQuery, String type, String status) {
        Page<WarningRecord> page = warningRecordMapper.selectPage(
                new Page<>(pageQuery.getPage(), pageQuery.getSize()),
                new LambdaQueryWrapper<WarningRecord>()
                        .eq(type != null && !type.isBlank(), WarningRecord::getWarningType, type)
                        .eq(status != null && !status.isBlank(), WarningRecord::getHandleStatus, status)
                        .orderByDesc(WarningRecord::getId)
        );
        return PageResult.from(page);
    }

    @Transactional(rollbackFor = Exception.class)
    @Scheduled(cron = "0 0/30 * * * ?")
    public void scan() {
        Instant startedAt = Instant.now();
        long createdWarnings = 0;
        createdWarnings += scanLowStock();
        createdWarnings += scanBacklog();
        createdWarnings += scanExpiring();
        createdWarnings += scanExpired();
        createdWarnings += scanAbnormalUsage();
        businessMetrics.recordWarningScan(Duration.between(startedAt, Instant.now()), createdWarnings);
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
        operationLogService.log(AuthUtil.currentUserId(), "WARNING", "HANDLE", "处理预警:" + id);
    }

    private long scanLowStock() {
        List<Inventory> inventories = inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>());
        Map<Long, MaterialInfo> materialMap = loadMaterialMap(inventories);
        long created = 0;
        for (Inventory inventory : inventories) {
            MaterialInfo material = materialMap.get(inventory.getMaterialId());
            if (material == null || material.getSafetyStock() == null) {
                continue;
            }
            if (inventory.getCurrentQty() < material.getSafetyStock()) {
                if (createWarningIfAbsent(WarningType.STOCK_LOW, inventory.getMaterialId(), inventory.getWarehouseId(),
                        "库存低于安全库存阈值，当前库存:" + inventory.getCurrentQty() + " 安全库存:" + material.getSafetyStock())) {
                    created++;
                }
            }
        }
        return created;
    }

    private long scanBacklog() {
        List<Inventory> inventories = inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>());
        Map<Long, MaterialInfo> materialMap = loadMaterialMap(inventories);
        long created = 0;
        for (Inventory inventory : inventories) {
            MaterialInfo material = materialMap.get(inventory.getMaterialId());
            if (material == null || material.getSafetyStock() == null) {
                continue;
            }
            if (inventory.getCurrentQty() > material.getSafetyStock() * 3) {
                if (createWarningIfAbsent(WarningType.STOCK_BACKLOG, inventory.getMaterialId(), inventory.getWarehouseId(),
                        "库存积压预警，当前库存量超过安全库存3倍")) {
                    created++;
                }
            }
        }
        return created;
    }

    private long scanExpiring() {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(30);
        long created = 0;
        List<InventoryBatch> batches = batchMapper.selectList(new LambdaQueryWrapper<InventoryBatch>()
                .gt(InventoryBatch::getRemainQty, 0)
                .ge(InventoryBatch::getExpireDate, today)
                .le(InventoryBatch::getExpireDate, threshold));
        for (InventoryBatch batch : batches) {
            if (createWarningIfAbsent(WarningType.EXPIRING_SOON, batch.getMaterialId(), batch.getWarehouseId(),
                    "物资即将过期，批次号:" + batch.getBatchNo() + " 过期日期:" + batch.getExpireDate())) {
                created++;
            }
        }
        return created;
    }

    private long scanExpired() {
        LocalDate today = LocalDate.now();
        long created = 0;
        List<InventoryBatch> batches = batchMapper.selectList(new LambdaQueryWrapper<InventoryBatch>()
                .gt(InventoryBatch::getRemainQty, 0)
                .lt(InventoryBatch::getExpireDate, today));
        for (InventoryBatch batch : batches) {
            if (createWarningIfAbsent(WarningType.EXPIRED, batch.getMaterialId(), batch.getWarehouseId(),
                    "物资已过期，批次号:" + batch.getBatchNo() + " 过期日期:" + batch.getExpireDate())) {
                created++;
            }
        }
        return created;
    }

    private long scanAbnormalUsage() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStart = now.minusDays(7);
        LocalDateTime monthStart = now.minusDays(30);
        List<StockOutItem> weekItems = stockOutItemMapper.selectList(new LambdaQueryWrapper<StockOutItem>()
                .ge(StockOutItem::getCreatedAt, weekStart));
        List<StockOutItem> monthItems = stockOutItemMapper.selectList(new LambdaQueryWrapper<StockOutItem>()
                .ge(StockOutItem::getCreatedAt, monthStart));

        final long[] created = {0};
        weekItems.stream().map(StockOutItem::getMaterialId).distinct().forEach(materialId -> {
            int weekSum = weekItems.stream().filter(it -> materialId.equals(it.getMaterialId())).mapToInt(StockOutItem::getQuantity).sum();
            int monthSum = monthItems.stream().filter(it -> materialId.equals(it.getMaterialId())).mapToInt(StockOutItem::getQuantity).sum();
            double avgWeek = monthSum / 4.0;
            if (avgWeek > 0 && weekSum > avgWeek * 1.5) {
                if (createWarningIfAbsent(WarningType.ABNORMAL_USAGE, materialId, null,
                        "近7天出库量异常，本周出库:" + weekSum + " 月均周出库:" + String.format("%.2f", avgWeek))) {
                    created[0]++;
                }
            }
        });
        return created[0];
    }

    private boolean createWarningIfAbsent(String type, Long materialId, Long warehouseId, String content) {
        WarningRecord existed = warningRecordMapper.selectOne(new LambdaQueryWrapper<WarningRecord>()
                .eq(WarningRecord::getWarningType, type)
                .eq(WarningRecord::getMaterialId, materialId)
                .eq(warehouseId != null, WarningRecord::getWarehouseId, warehouseId)
                .eq(WarningRecord::getHandleStatus, "UNHANDLED")
                .last("limit 1"));
        if (existed != null) {
            return false;
        }
        WarningRecord warningRecord = new WarningRecord();
        warningRecord.setWarningType(type);
        warningRecord.setMaterialId(materialId);
        warningRecord.setWarehouseId(warehouseId);
        warningRecord.setContent(content);
        warningRecord.setHandleStatus("UNHANDLED");
        warningRecordMapper.insert(warningRecord);
        return true;
    }

    private Map<Long, MaterialInfo> loadMaterialMap(List<Inventory> inventories) {
        List<Long> materialIds = inventories.stream().map(Inventory::getMaterialId).distinct().toList();
        if (materialIds.isEmpty()) {
            return new HashMap<>();
        }
        return materialInfoMapper.selectBatchIds(materialIds).stream()
                .collect(Collectors.toMap(MaterialInfo::getId, Function.identity()));
    }
}
