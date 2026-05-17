package com.campus.material.modules.material.controller;

import com.campus.material.common.ApiResponse;
import com.campus.material.modules.material.entity.MaterialCategory;
import com.campus.material.modules.material.entity.MaterialInfo;
import com.campus.material.modules.material.service.MaterialService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/material")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping("/category")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','PURCHASER')")
    public ApiResponse<List<MaterialCategory>> categoryList() {
        return ApiResponse.ok(materialService.listCategory());
    }

    @PostMapping("/category")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN')")
    public ApiResponse<MaterialCategory> saveCategory(@RequestBody MaterialCategory category) {
        return ApiResponse.ok(materialService.saveCategory(category));
    }

    @DeleteMapping("/category/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        materialService.deleteCategory(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/info")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER','DEPT_USER','USER','PURCHASER','DISPATCHER')")
    public ApiResponse<List<MaterialInfo>> materialList(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(materialService.listMaterial(keyword));
    }

    @PostMapping("/info")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','PURCHASER')")
    public ApiResponse<MaterialInfo> saveMaterial(@RequestBody MaterialInfo materialInfo) {
        return ApiResponse.ok(materialService.saveMaterial(materialInfo));
    }

    @DeleteMapping("/info/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteMaterial(@PathVariable Long id) {
        materialService.deleteMaterial(id);
        return ApiResponse.ok(null);
    }
}
