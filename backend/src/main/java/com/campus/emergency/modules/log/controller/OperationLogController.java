package com.campus.emergency.modules.log.controller;

import com.campus.emergency.common.ApiResponse;
import com.campus.emergency.modules.log.entity.OperationLog;
import com.campus.emergency.modules.log.service.OperationLogQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/log")
public class OperationLogController {

    private final OperationLogQueryService queryService;

    public OperationLogController(OperationLogQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER')")
    public ApiResponse<List<OperationLog>> list() {
        return ApiResponse.ok(queryService.list());
    }
}
