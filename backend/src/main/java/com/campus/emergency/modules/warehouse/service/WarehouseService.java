package com.campus.emergency.modules.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.emergency.modules.log.service.OperationLogService;
import com.campus.emergency.modules.warehouse.entity.Warehouse;
import com.campus.emergency.modules.warehouse.mapper.WarehouseMapper;
import com.campus.emergency.security.AuthUtil;
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
}
