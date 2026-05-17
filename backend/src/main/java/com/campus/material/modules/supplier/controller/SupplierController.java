package com.campus.material.modules.supplier.controller;

import com.campus.material.common.ApiResponse;
import com.campus.material.modules.supplier.entity.Supplier;
import com.campus.material.modules.supplier.service.SupplierService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/supplier")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','PURCHASER')")
    public ApiResponse<List<Supplier>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(supplierService.list(keyword));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','PURCHASER')")
    public ApiResponse<Void> save(@RequestBody Supplier supplier) {
        supplierService.save(supplier);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return ApiResponse.ok(null);
    }
}
