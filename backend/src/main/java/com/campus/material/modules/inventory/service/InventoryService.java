package com.campus.material.modules.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.material.common.BizException;
import com.campus.material.common.OrderStatus;
import com.campus.material.common.PageQuery;
import com.campus.material.common.PageResult;
import com.campus.material.common.WarningType;
import com.campus.material.monitoring.BusinessMetrics;
import com.campus.material.modules.apply.entity.ApplyOrder;
import com.campus.material.modules.apply.entity.ApplyOrderItem;
import com.campus.material.modules.apply.mapper.ApplyOrderItemMapper;
import com.campus.material.modules.apply.mapper.ApplyOrderMapper;
import com.campus.material.modules.inventory.dto.InventoryAdjustRequest;
import com.campus.material.modules.inventory.dto.StockInRequest;
import com.campus.material.modules.inventory.dto.StockOutRequest;
import com.campus.material.modules.inventory.entity.*;
import com.campus.material.modules.inventory.mapper.*;
import com.campus.material.modules.log.service.OperationLogService;
import com.campus.material.modules.material.entity.MaterialInfo;
import com.campus.material.modules.material.mapper.MaterialInfoMapper;
import com.campus.material.modules.warning.entity.WarningRecord;
import com.campus.material.modules.warning.mapper.WarningRecordMapper;
import com.campus.material.security.AuthUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
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
    private final BusinessMetrics businessMetrics;

    public InventoryService(InventoryMapper inventoryMapper, InventoryBatchMapper batchMapper, StockInMapper stockInMapper,
                            StockInItemMapper stockInItemMapper, StockOutMapper stockOutMapper, StockOutItemMapper stockOutItemMapper,
                            MaterialInfoMapper materialInfoMapper, WarningRecordMapper warningRecordMapper,
                            ApplyOrderMapper applyOrderMapper, ApplyOrderItemMapper applyOrderItemMapper,
                            OperationLogService operationLogService, BusinessMetrics businessMetrics) {
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
        this.businessMetrics = businessMetrics;
    }

    public PageResult<Map<String, Object>> list(PageQuery pageQuery, Long materialId, Long warehouseId) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<Inventory>().orderByDesc(Inventory::getId);
        if (materialId != null) {
            wrapper.eq(Inventory::getMaterialId, materialId);
        }
        if (warehouseId != null) {
            wrapper.eq(Inventory::getWarehouseId, warehouseId);
        }
        Page<Inventory> inventoryPage = inventoryMapper.selectPage(new Page<>(pageQuery.getPage(), pageQuery.getSize()), wrapper);
        List<Inventory> inventoryList = inventoryPage.getRecords();
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
        return PageResult.of(result, inventoryPage.getTotal(), inventoryPage.getCurrent(), inventoryPage.getSize());
    }

    public List<InventoryBatch> batches(Long materialId, Long warehouseId) {
        return batchMapper.selectList(new LambdaQueryWrapper<InventoryBatch>()
                .eq(materialId != null, InventoryBatch::getMaterialId, materialId)
                .eq(warehouseId != null, InventoryBatch::getWarehouseId, warehouseId)
                .gt(InventoryBatch::getRemainQty, 0)
                .orderByAsc(InventoryBatch::getExpireDate)
                .orderByAsc(InventoryBatch::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public InventoryBatch saveBatch(InventoryBatch batch) {
        validateBatch(batch);
        normalizeBatch(batch);

        if (batch.getId() == null) {
            batchMapper.insert(batch);
            updateInventoryQuantity(batch.getMaterialId(), batch.getWarehouseId(), safeQty(batch.getRemainQty()));
            operationLogService.log(AuthUtil.currentUserId(), "INVENTORY", "CREATE_BATCH", "批次:" + batch.getBatchNo());
            return batchMapper.selectById(batch.getId());
        }

        InventoryBatch old = batchMapper.selectById(batch.getId());
        if (old == null) {
            throw new BizException("批次记录不存在");
        }
        ensureUpdated(batchMapper.updateById(batch), "批次信息更新失败，请重试");
        if (Objects.equals(old.getMaterialId(), batch.getMaterialId()) && Objects.equals(old.getWarehouseId(), batch.getWarehouseId())) {
            updateInventoryQuantity(batch.getMaterialId(), batch.getWarehouseId(), safeQty(batch.getRemainQty()) - safeQty(old.getRemainQty()));
        } else {
            updateInventoryQuantity(old.getMaterialId(), old.getWarehouseId(), -safeQty(old.getRemainQty()));
            updateInventoryQuantity(batch.getMaterialId(), batch.getWarehouseId(), safeQty(batch.getRemainQty()));
        }
        operationLogService.log(AuthUtil.currentUserId(), "INVENTORY", "UPDATE_BATCH", "批次:" + batch.getBatchNo());
        return batchMapper.selectById(batch.getId());
    }

    public PageResult<StockIn> listStockIn(PageQuery pageQuery) {
        Page<StockIn> page = stockInMapper.selectPage(
                new Page<>(pageQuery.getPage(), pageQuery.getSize()),
                new LambdaQueryWrapper<StockIn>().orderByDesc(StockIn::getId)
        );
        return PageResult.from(page);
    }

    public PageResult<StockOut> listStockOut(PageQuery pageQuery) {
        Page<StockOut> page = stockOutMapper.selectPage(
                new Page<>(pageQuery.getPage(), pageQuery.getSize()),
                new LambdaQueryWrapper<StockOut>().orderByDesc(StockOut::getId)
        );
        return PageResult.from(page);
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
            ensureUpdated(inventoryMapper.updateById(inventory), "库存汇总更新失败，请重试");

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
        businessMetrics.recordInventoryStockIn();
    }

    @Transactional(rollbackFor = Exception.class)
    public void stockOut(StockOutRequest request) {
        StockOut stockOut = new StockOut();
        stockOut.setApplyOrderId(request.getApplyOrderId());
        stockOut.setWarehouseId(request.getWarehouseId());
        stockOut.setOperatorId(AuthUtil.currentUserId());
        stockOut.setRemark(request.getRemark());
        stockOutMapper.insert(stockOut);
        Map<Long, ApplyOrderItem> applyItemMap = loadApplyItemsByMaterial(request.getApplyOrderId());
        ApplyOrder applyOrder = loadApplyOrderForOutbound(request.getApplyOrderId(), request.getWarehouseId());

        /*
         * 出库链路需要同时维护批次余量、库存汇总以及申领实发数量的一致性。
         * 处理顺序固定为：校验库存 -> 按 FEFO 扣减批次 -> 更新库存汇总 -> 回写申领实发数量。
         * 申领单明细在事务开始时一次性预取，避免按物资逐条查询带来的重复回表。
         */
        for (StockOutRequest.Item item : request.getItems()) {
            Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                    .eq(Inventory::getMaterialId, item.getMaterialId())
                    .eq(Inventory::getWarehouseId, request.getWarehouseId())
                    .last("limit 1"));
            int availableQty = inventory == null ? 0 : inventory.getCurrentQty() - inventory.getLockedQty();
            if (inventory == null || (!isApplyOutbound(request) && availableQty < item.getQuantity()) || (isApplyOutbound(request) && inventory.getCurrentQty() < item.getQuantity())) {
                createLowStockWarning(item.getMaterialId(), request.getWarehouseId(), "出库时库存不足");
                throw new BizException("物资ID " + item.getMaterialId() + " 库存不足");
            }
            if (isApplyOutbound(request)) {
                ApplyOrderItem applyItem = requireApplyItem(applyItemMap, item.getMaterialId());
                int remainingReserved = applyItem.getApplyQty() - safeQty(applyItem.getActualQty());
                if (remainingReserved < item.getQuantity()) {
                    throw new BizException("物资ID " + item.getMaterialId() + " 超出申领单已锁定数量");
                }
                if (inventory.getLockedQty() < item.getQuantity()) {
                    throw new BizException("物资ID " + item.getMaterialId() + " 锁定库存不足，请刷新后重试");
                }
            }

            List<InventoryBatch> candidates = batchMapper.selectList(new LambdaQueryWrapper<InventoryBatch>()
                    .eq(InventoryBatch::getMaterialId, item.getMaterialId())
                    .eq(InventoryBatch::getWarehouseId, request.getWarehouseId())
                    .gt(InventoryBatch::getRemainQty, 0)
                    .and(wrapper -> wrapper.isNull(InventoryBatch::getExpireDate)
                            .or()
                            .ge(InventoryBatch::getExpireDate, LocalDate.now()))
                    .orderByAsc(InventoryBatch::getExpireDate)
                    .orderByAsc(InventoryBatch::getId));

            int remain = item.getQuantity();
            for (InventoryBatch batch : candidates) {
                if (remain <= 0) {
                    break;
                }
                int use = Math.min(remain, batch.getRemainQty());
                batch.setRemainQty(batch.getRemainQty() - use);
                ensureUpdated(batchMapper.updateById(batch), "批次库存已被其他操作修改，请重试");
                remain -= use;
            }
            if (remain > 0) {
                throw new BizException("批次库存不足，无法完成出库操作");
            }

            inventory.setCurrentQty(inventory.getCurrentQty() - item.getQuantity());
            if (isApplyOutbound(request)) {
                inventory.setLockedQty(inventory.getLockedQty() - item.getQuantity());
            }
            ensureUpdated(inventoryMapper.updateById(inventory), "库存已被其他操作修改，请重试");

            StockOutItem outItem = new StockOutItem();
            outItem.setStockOutId(stockOut.getId());
            outItem.setMaterialId(item.getMaterialId());
            outItem.setQuantity(item.getQuantity());
            stockOutItemMapper.insert(outItem);

            if (request.getApplyOrderId() != null) {
                ApplyOrderItem applyItem = requireApplyItem(applyItemMap, item.getMaterialId());
                int old = safeQty(applyItem.getActualQty());
                applyItem.setActualQty(old + item.getQuantity());
                ensureUpdated(applyOrderItemMapper.updateById(applyItem), "申领实发数量更新失败，请重试");
            }

            checkSafetyWarning(item.getMaterialId(), request.getWarehouseId());
        }

        if (applyOrder != null) {
            applyOrder.setStatus(OrderStatus.OUTBOUND);
            ensureUpdated(applyOrderMapper.updateById(applyOrder), "申领单状态更新失败，请重试");
        }
        operationLogService.log(AuthUtil.currentUserId(), "INVENTORY", "STOCK_OUT", "出库单:" + stockOut.getId());
        businessMetrics.recordInventoryStockOut();
    }

    @Transactional(rollbackFor = Exception.class)
    public void adjust(InventoryAdjustRequest request) {
        Inventory inventory = inventoryMapper.selectById(request.getInventoryId());
        if (inventory == null) {
            throw new BizException("库存记录不存在");
        }
        inventory.setCurrentQty(request.getActualQty());
        ensureUpdated(inventoryMapper.updateById(inventory), "库存盘点更新失败，请重试");
        operationLogService.log(AuthUtil.currentUserId(), "INVENTORY", "CHECK", "库存盘点ID:" + request.getInventoryId());
    }

    public List<InventoryBatch> recommendOutbound(Long materialId, Long warehouseId) {
        return batchMapper.selectList(new LambdaQueryWrapper<InventoryBatch>()
                .eq(InventoryBatch::getMaterialId, materialId)
                .eq(InventoryBatch::getWarehouseId, warehouseId)
                .gt(InventoryBatch::getRemainQty, 0)
                .and(wrapper -> wrapper.isNull(InventoryBatch::getExpireDate)
                        .or()
                        .ge(InventoryBatch::getExpireDate, LocalDate.now()))
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

    public Long reserveForApplyOrder(List<ApplyOrderItem> applyItems) {
        Map<Long, Integer> requestByMaterial = aggregateApplyQuantities(applyItems);
        if (requestByMaterial.isEmpty()) {
            throw new BizException("申领单缺少物资明细");
        }

        List<Inventory> inventoryRows = inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>()
                .in(Inventory::getMaterialId, requestByMaterial.keySet()));
        Long warehouseId = selectReservationWarehouse(inventoryRows, requestByMaterial);
        if (warehouseId == null) {
            throw new BizException("库存不足或未找到可满足申领的仓库");
        }

        Map<Long, Inventory> inventoryByMaterial = inventoryRows.stream()
                .filter(row -> warehouseId.equals(row.getWarehouseId()))
                .collect(Collectors.toMap(Inventory::getMaterialId, Function.identity()));

        for (Map.Entry<Long, Integer> entry : requestByMaterial.entrySet()) {
            Inventory inventory = inventoryByMaterial.get(entry.getKey());
            if (inventory == null) {
                throw new BizException("物资ID " + entry.getKey() + " 库存记录不存在");
            }
            ensureUpdated(
                    inventoryMapper.reserveLockedQty(inventory.getId(), entry.getValue(), safeQty(inventory.getVersion())),
                    "库存已被其他申请占用，请刷新后重试"
            );
        }
        return warehouseId;
    }

    public void releaseApplyReservation(Long warehouseId, List<ApplyOrderItem> applyItems) {
        if (warehouseId == null) {
            return;
        }
        Map<Long, Integer> releaseByMaterial = aggregateRemainingReservation(applyItems);
        if (releaseByMaterial.isEmpty()) {
            return;
        }

        List<Inventory> inventoryRows = inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getWarehouseId, warehouseId)
                .in(Inventory::getMaterialId, releaseByMaterial.keySet()));
        Map<Long, Inventory> inventoryByMaterial = inventoryRows.stream()
                .collect(Collectors.toMap(Inventory::getMaterialId, Function.identity()));

        for (Map.Entry<Long, Integer> entry : releaseByMaterial.entrySet()) {
            Inventory inventory = inventoryByMaterial.get(entry.getKey());
            if (inventory == null) {
                throw new BizException("物资ID " + entry.getKey() + " 库存记录不存在，无法释放锁定库存");
            }
            ensureUpdated(
                    inventoryMapper.releaseLockedQty(inventory.getId(), entry.getValue(), safeQty(inventory.getVersion())),
                    "锁定库存释放失败，请刷新后重试"
            );
        }
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

    private Map<Long, ApplyOrderItem> loadApplyItemsByMaterial(Long applyOrderId) {
        if (applyOrderId == null) {
            return Map.of();
        }
        return applyOrderItemMapper.selectList(new LambdaQueryWrapper<ApplyOrderItem>()
                        .eq(ApplyOrderItem::getApplyOrderId, applyOrderId))
                .stream()
                .collect(Collectors.toMap(
                        ApplyOrderItem::getMaterialId,
                        Function.identity(),
                        (left, right) -> left
                ));
    }

    private Map<Long, Integer> aggregateApplyQuantities(List<ApplyOrderItem> applyItems) {
        return applyItems.stream().collect(Collectors.toMap(
                ApplyOrderItem::getMaterialId,
                item -> safeQty(item.getApplyQty()),
                Integer::sum,
                LinkedHashMap::new
        ));
    }

    private Map<Long, Integer> aggregateRemainingReservation(List<ApplyOrderItem> applyItems) {
        Map<Long, Integer> remaining = new LinkedHashMap<>();
        for (ApplyOrderItem item : applyItems) {
            int reservedQty = safeQty(item.getApplyQty()) - safeQty(item.getActualQty());
            if (reservedQty > 0) {
                remaining.merge(item.getMaterialId(), reservedQty, Integer::sum);
            }
        }
        return remaining;
    }

    private Long selectReservationWarehouse(List<Inventory> inventoryRows, Map<Long, Integer> requestByMaterial) {
        Map<Long, Map<Long, Inventory>> inventoryByWarehouse = inventoryRows.stream()
                .collect(Collectors.groupingBy(Inventory::getWarehouseId, Collectors.toMap(Inventory::getMaterialId, Function.identity(), (left, right) -> left)));

        return inventoryByWarehouse.entrySet().stream()
                .filter(entry -> canSatisfyWarehouse(entry.getValue(), requestByMaterial))
                .map(entry -> Map.entry(entry.getKey(), warehouseSlack(entry.getValue(), requestByMaterial)))
                .sorted((left, right) -> {
                    int bySlack = Integer.compare(right.getValue(), left.getValue());
                    return bySlack != 0 ? bySlack : Long.compare(left.getKey(), right.getKey());
                })
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private boolean canSatisfyWarehouse(Map<Long, Inventory> inventoryByMaterial, Map<Long, Integer> requestByMaterial) {
        for (Map.Entry<Long, Integer> entry : requestByMaterial.entrySet()) {
            Inventory inventory = inventoryByMaterial.get(entry.getKey());
            if (inventory == null || inventory.getCurrentQty() - inventory.getLockedQty() < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private int warehouseSlack(Map<Long, Inventory> inventoryByMaterial, Map<Long, Integer> requestByMaterial) {
        int slack = 0;
        for (Map.Entry<Long, Integer> entry : requestByMaterial.entrySet()) {
            Inventory inventory = inventoryByMaterial.get(entry.getKey());
            slack += inventory.getCurrentQty() - inventory.getLockedQty() - entry.getValue();
        }
        return slack;
    }

    private ApplyOrder loadApplyOrderForOutbound(Long applyOrderId, Long warehouseId) {
        if (applyOrderId == null) {
            return null;
        }
        ApplyOrder applyOrder = applyOrderMapper.selectById(applyOrderId);
        if (applyOrder == null) {
            throw new BizException("申领单不存在");
        }
        if (!Set.of(OrderStatus.APPROVED, OrderStatus.OUTBOUND).contains(applyOrder.getStatus())) {
            throw new BizException("当前申领单状态不允许出库");
        }
        if (applyOrder.getReservedWarehouseId() != null && !applyOrder.getReservedWarehouseId().equals(warehouseId)) {
            throw new BizException("申领单已锁定至其他仓库，请按锁定仓库执行出库");
        }
        return applyOrder;
    }

    private ApplyOrderItem requireApplyItem(Map<Long, ApplyOrderItem> applyItemMap, Long materialId) {
        ApplyOrderItem item = applyItemMap.get(materialId);
        if (item == null) {
            throw new BizException("申领单中不存在物资ID " + materialId);
        }
        return item;
    }

    private boolean isApplyOutbound(StockOutRequest request) {
        return request.getApplyOrderId() != null;
    }

    private void validateBatch(InventoryBatch batch) {
        if (batch.getMaterialId() == null) {
            throw new BizException(400, "物资不能为空");
        }
        if (batch.getWarehouseId() == null) {
            throw new BizException(400, "仓库不能为空");
        }
        if (batch.getBatchNo() == null || batch.getBatchNo().isBlank()) {
            throw new BizException(400, "批次号不能为空");
        }
    }

    private void normalizeBatch(InventoryBatch batch) {
        batch.setBatchNo(batch.getBatchNo().trim());
        int inQty = safeQty(batch.getInQty());
        int remainQty = batch.getRemainQty() == null ? inQty : safeQty(batch.getRemainQty());
        if (inQty < 0 || remainQty < 0) {
            throw new BizException(400, "批次数量不能小于0");
        }
        if (remainQty > inQty) {
            throw new BizException(400, "剩余数量不能大于入库数量");
        }
        batch.setInQty(inQty);
        batch.setRemainQty(remainQty);
    }

    private void updateInventoryQuantity(Long materialId, Long warehouseId, int delta) {
        if (delta == 0) {
            return;
        }
        Inventory inventory = getOrInitInventory(materialId, warehouseId);
        int nextQty = safeQty(inventory.getCurrentQty()) + delta;
        if (nextQty < 0 || nextQty < safeQty(inventory.getLockedQty())) {
            throw new BizException("库存汇总数量不足，无法同步批次变更");
        }
        inventory.setCurrentQty(nextQty);
        ensureUpdated(inventoryMapper.updateById(inventory), "库存汇总更新失败，请重试");
    }

    private int safeQty(Integer quantity) {
        return quantity == null ? 0 : quantity;
    }

    private void ensureUpdated(int updatedRows, String message) {
        if (updatedRows != 1) {
            throw new BizException(message);
        }
    }
}
