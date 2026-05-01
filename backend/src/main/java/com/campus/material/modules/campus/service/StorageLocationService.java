package com.campus.material.modules.campus.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.material.modules.campus.entity.StorageLocation;
import com.campus.material.modules.campus.mapper.StorageLocationMapper;
import com.campus.material.modules.log.service.OperationLogService;
import com.campus.material.security.AuthUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StorageLocationService {

    private final StorageLocationMapper locationMapper;
    private final OperationLogService logService;

    public StorageLocationService(StorageLocationMapper locationMapper, OperationLogService logService) {
        this.locationMapper = locationMapper;
        this.logService = logService;
    }

    public List<StorageLocation> list(Long warehouseId) {
        LambdaQueryWrapper<StorageLocation> qw = new LambdaQueryWrapper<>();
        if (warehouseId != null) {
            qw.eq(StorageLocation::getWarehouseId, warehouseId);
        }
        return locationMapper.selectList(qw.orderByAsc(StorageLocation::getLocationCode));
    }

    public void save(StorageLocation location) {
        Long uid = AuthUtil.currentUserId();
        if (location.getId() == null) {
            locationMapper.insert(location);
            logService.log(uid, "LOCATION", "CREATE", "閺傛澘顤冩惔鎾茬秴:" + location.getLocationCode());
        } else {
            locationMapper.updateById(location);
            logService.log(uid, "LOCATION", "UPDATE", "娣囶喗鏁兼惔鎾茬秴:" + location.getId());
        }
    }

    public void delete(Long id) {
        locationMapper.deleteById(id);
        logService.log(AuthUtil.currentUserId(), "LOCATION", "DELETE", "閸掔娀娅庢惔鎾茬秴:" + id);
    }
}
