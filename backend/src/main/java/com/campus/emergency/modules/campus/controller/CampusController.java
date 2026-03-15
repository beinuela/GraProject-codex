package com.campus.emergency.modules.campus.controller;

import com.campus.emergency.common.ApiResponse;
import com.campus.emergency.modules.campus.entity.Campus;
import com.campus.emergency.modules.campus.service.CampusService;
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
