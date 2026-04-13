package com.campus.material.modules.supplier.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.material.modules.log.service.OperationLogService;
import com.campus.material.modules.supplier.entity.Supplier;
import com.campus.material.modules.supplier.mapper.SupplierMapper;
import com.campus.material.security.AuthUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SupplierService {

    private final SupplierMapper supplierMapper;
    private final OperationLogService logService;

    public SupplierService(SupplierMapper supplierMapper, OperationLogService logService) {
        this.supplierMapper = supplierMapper;
        this.logService = logService;
    }

    public List<Supplier> list(String keyword) {
        LambdaQueryWrapper<Supplier> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.like(Supplier::getSupplierName, keyword)
              .or().like(Supplier::getSupplyScope, keyword);
        }
        return supplierMapper.selectList(qw.orderByDesc(Supplier::getId));
    }

    public void save(Supplier supplier) {
        Long uid = AuthUtil.currentUserId();
        if (supplier.getId() == null) {
            supplierMapper.insert(supplier);
            logService.log(uid, "SUPPLIER", "CREATE", "閺傛澘顤冩笟娑樼安閸?" + supplier.getSupplierName());
        } else {
            supplierMapper.updateById(supplier);
            logService.log(uid, "SUPPLIER", "UPDATE", "娣囶喗鏁兼笟娑樼安閸?" + supplier.getId());
        }
    }

    public void delete(Long id) {
        supplierMapper.deleteById(id);
        logService.log(AuthUtil.currentUserId(), "SUPPLIER", "DELETE", "閸掔娀娅庢笟娑樼安閸?" + id);
    }
}
