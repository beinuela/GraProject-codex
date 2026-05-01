package com.campus.material.modules.campus.controller;

import com.campus.material.common.ApiResponse;
import com.campus.material.modules.campus.entity.Campus;
import com.campus.material.modules.campus.service.CampusService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campus")
public class CampusController {

    private final CampusService campusService;

    public CampusController(CampusService campusService) {
        this.campusService = campusService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER','DEPT_USER')")
    public ApiResponse<List<Campus>> list() {
        return ApiResponse.ok(campusService.list());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<Void> save(@RequestBody Campus campus) {
        campusService.save(campus);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        campusService.delete(id);
        return ApiResponse.ok(null);
    }
}
