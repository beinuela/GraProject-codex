package com.campus.material.modules.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.material.common.BizException;
import com.campus.material.modules.log.service.OperationLogService;
import com.campus.material.modules.warehouse.entity.Warehouse;
import com.campus.material.modules.warehouse.mapper.WarehouseMapper;
import com.campus.material.security.AuthUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WarehouseService {

    private final WarehouseMapper warehouseMapper;
    private final OperationLogService operationLogService;

    public WarehouseService(WarehouseMapper warehouseMapper, OperationLogService operationLogService) {
        this.warehouseMapper = warehouseMapper;
        this.operationLogService = operationLogService;
    }

    public List<Warehouse> list() {
        return warehouseMapper.selectList(new LambdaQueryWrapper<Warehouse>().orderByAsc(Warehouse::getId));
    }

    public Warehouse save(Warehouse warehouse) {
        validateWarehouse(warehouse);
        if (warehouse.getId() == null) {
            warehouseMapper.insert(warehouse);
            operationLogService.log(AuthUtil.currentUserId(), "WAREHOUSE", "CREATE", warehouse.getWarehouseName());
        } else {
            warehouseMapper.updateById(warehouse);
            operationLogService.log(AuthUtil.currentUserId(), "WAREHOUSE", "UPDATE", warehouse.getWarehouseName());
        }
        return warehouseMapper.selectById(warehouse.getId());
    }

    public void delete(Long id) {
        warehouseMapper.deleteById(id);
        operationLogService.log(AuthUtil.currentUserId(), "WAREHOUSE", "DELETE", String.valueOf(id));
    }

    private void validateWarehouse(Warehouse warehouse) {
        if (warehouse.getWarehouseName() == null || warehouse.getWarehouseName().isBlank()) {
            throw new BizException(400, "仓库名称不能为空");
        }
        String warehouseName = warehouse.getWarehouseName().trim();
        Warehouse duplicated = warehouseMapper.selectOne(new LambdaQueryWrapper<Warehouse>()
                .eq(Warehouse::getWarehouseName, warehouseName)
                .ne(warehouse.getId() != null, Warehouse::getId, warehouse.getId())
                .last("limit 1"));
        if (duplicated != null) {
            throw new BizException(409, "仓库名称已存在，请勿重复添加");
        }
        warehouse.setWarehouseName(warehouseName);
        if (warehouse.getWarehouseCode() != null) {
            warehouse.setWarehouseCode(warehouse.getWarehouseCode().trim());
        }
        if (warehouse.getStatus() == null || warehouse.getStatus().isBlank()) {
            warehouse.setStatus("NORMAL");
        }
    }
}
