package com.campus.material.modules.warehouse.controller;

import com.campus.material.common.ApiResponse;
import com.campus.material.modules.warehouse.entity.Warehouse;
import com.campus.material.modules.warehouse.service.WarehouseService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER')")
    public ApiResponse<List<Warehouse>> list() {
        return ApiResponse.ok(warehouseService.list());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN')")
    public ApiResponse<Warehouse> save(@RequestBody Warehouse warehouse) {
        return ApiResponse.ok(warehouseService.save(warehouse));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return ApiResponse.ok(null);
    }
}
