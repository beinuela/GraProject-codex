package com.campus.material.modules.apply.controller;

import com.campus.material.common.ApiResponse;
import com.campus.material.common.PageQuery;
import com.campus.material.common.PageResult;
import com.campus.material.common.RemarkRequest;
import com.campus.material.modules.apply.dto.ApplyCreateRequest;
import com.campus.material.modules.apply.entity.ApplyOrder;
import com.campus.material.modules.apply.service.ApplyService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/apply")
public class ApplyController {

    private final ApplyService applyService;
    private final com.campus.material.modules.log.service.OperationLogService operationLogService;

    public ApplyController(ApplyService applyService, com.campus.material.modules.log.service.OperationLogService operationLogService) {
        this.applyService = applyService;
        this.operationLogService = operationLogService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER','DEPT_USER','USER','DISPATCHER')")
    public ApiResponse<PageResult<ApplyOrder>> list(@Valid PageQuery pageQuery) {
        return ApiResponse.ok(applyService.list(pageQuery));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER','DEPT_USER','USER','DISPATCHER')")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(applyService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEPT_USER','USER')")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody ApplyCreateRequest request) {
        return ApiResponse.ok(applyService.create(request));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN','DEPT_USER','USER')")
    public ApiResponse<Void> submit(@PathVariable Long id) {
        applyService.submit(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','APPROVER')")
    public ApiResponse<Void> approve(@PathVariable Long id,
                                     @RequestParam(required = false) String remark,
                                     @RequestBody(required = false) RemarkRequest body) {
        applyService.approve(id, resolveRemark(remark, body));
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','APPROVER')")
    public ApiResponse<Void> reject(@PathVariable Long id,
                                    @RequestParam(required = false) String remark,
                                    @RequestBody(required = false) RemarkRequest body) {
        applyService.reject(id, resolveRemark(remark, body));
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('ADMIN','DEPT_USER','USER')")
    public ApiResponse<Void> receive(@PathVariable Long id) {
        applyService.receive(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/timeline")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER','DEPT_USER','USER','DISPATCHER')")
    public ApiResponse<List<com.campus.material.modules.log.entity.OperationLog>> timeline(@PathVariable Long id) {
        return ApiResponse.ok(operationLogService.getTimeline("APPLY", id));
    }

    private String resolveRemark(String remark, RemarkRequest body) {
        if (body != null && body.getRemark() != null && !body.getRemark().isBlank()) {
            return body.getRemark();
        }
        return remark;
    }
}
