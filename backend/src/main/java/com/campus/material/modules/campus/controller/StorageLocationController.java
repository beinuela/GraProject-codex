package com.campus.material.modules.campus.controller;

import com.campus.material.common.ApiResponse;
import com.campus.material.modules.campus.entity.StorageLocation;
import com.campus.material.modules.campus.service.StorageLocationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/location")
public class StorageLocationController {

    private final StorageLocationService locationService;

    public StorageLocationController(StorageLocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN')")
    public ApiResponse<List<StorageLocation>> list(@RequestParam(required = false) Long warehouseId) {
        return ApiResponse.ok(locationService.list(warehouseId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN')")
    public ApiResponse<Void> save(@RequestBody StorageLocation location) {
        locationService.save(location);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        locationService.delete(id);
        return ApiResponse.ok(null);
    }
}
