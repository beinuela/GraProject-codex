package com.campus.emergency.modules.inventory.controller;

import com.campus.emergency.common.ApiResponse;
import com.campus.emergency.modules.inventory.dto.InventoryAdjustRequest;
import com.campus.emergency.modules.inventory.dto.StockInRequest;
import com.campus.emergency.modules.inventory.dto.StockOutRequest;
import com.campus.emergency.modules.inventory.entity.InventoryBatch;
import com.campus.emergency.modules.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER')")
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(required = false) Long materialId,
                                                       @RequestParam(required = false) Long warehouseId) {
        return ApiResponse.ok(inventoryService.list(materialId, warehouseId));
    }

    @GetMapping("/batches")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER')")
    public ApiResponse<List<InventoryBatch>> batches(@RequestParam(required = false) Long materialId,
                                                     @RequestParam(required = false) Long warehouseId) {
        return ApiResponse.ok(inventoryService.batches(materialId, warehouseId));
    }

    @PostMapping("/stock-in")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN')")
    public ApiResponse<Void> stockIn(@Valid @RequestBody StockInRequest request) {
        inventoryService.stockIn(request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/stock-out")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN')")
    public ApiResponse<Void> stockOut(@Valid @RequestBody StockOutRequest request) {
        inventoryService.stockOut(request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/check")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN')")
    public ApiResponse<Void> check(@Valid @RequestBody InventoryAdjustRequest request) {
        inventoryService.adjust(request);
        return ApiResponse.ok(null);
    }

    @GetMapping("/recommend-outbound")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN')")
    public ApiResponse<List<InventoryBatch>> recommendOutbound(@RequestParam Long materialId,
                                                               @RequestParam Long warehouseId) {
        return ApiResponse.ok(inventoryService.recommendOutbound(materialId, warehouseId));
    }
}
