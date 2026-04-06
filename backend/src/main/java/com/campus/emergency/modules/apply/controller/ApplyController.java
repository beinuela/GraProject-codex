package com.campus.emergency.modules.apply.controller;

import com.campus.emergency.common.ApiResponse;
import com.campus.emergency.modules.apply.dto.ApplyCreateRequest;
import com.campus.emergency.modules.apply.entity.ApplyOrder;
import com.campus.emergency.modules.apply.service.ApplyService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/apply")
public class ApplyController {

    private final ApplyService applyService;
    private final com.campus.emergency.modules.log.service.OperationLogService operationLogService;

    public ApplyController(ApplyService applyService, com.campus.emergency.modules.log.service.OperationLogService operationLogService) {
        this.applyService = applyService;
        this.operationLogService = operationLogService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER','DEPT_USER')")
    public ApiResponse<List<ApplyOrder>> list() {
        return ApiResponse.ok(applyService.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER','DEPT_USER')")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(applyService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEPT_USER')")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody ApplyCreateRequest request) {
        return ApiResponse.ok(applyService.create(request));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN','DEPT_USER')")
    public ApiResponse<Void> submit(@PathVariable Long id) {
        applyService.submit(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','APPROVER')")
    public ApiResponse<Void> approve(@PathVariable Long id, @RequestParam(required = false) String remark) {
        applyService.approve(id, remark);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','APPROVER')")
    public ApiResponse<Void> reject(@PathVariable Long id, @RequestParam(required = false) String remark) {
        applyService.reject(id, remark);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('ADMIN','DEPT_USER')")
    public ApiResponse<Void> receive(@PathVariable Long id) {
        applyService.receive(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/timeline")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER','DEPT_USER')")
    public ApiResponse<List<com.campus.emergency.modules.log.entity.OperationLog>> timeline(@PathVariable Long id) {
        return ApiResponse.ok(operationLogService.getTimeline("APPLY", id));
    }
}
