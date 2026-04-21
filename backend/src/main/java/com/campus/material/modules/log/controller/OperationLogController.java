package com.campus.material.modules.log.controller;

import com.campus.material.common.ApiResponse;
import com.campus.material.common.PageQuery;
import com.campus.material.common.PageResult;
import com.campus.material.modules.log.entity.OperationLog;
import com.campus.material.modules.log.service.OperationLogQueryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/log")
public class OperationLogController {

    private final OperationLogQueryService queryService;

    public OperationLogController(OperationLogQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER')")
    public ApiResponse<PageResult<OperationLog>> list(@Valid PageQuery pageQuery,
                                                      @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(queryService.list(pageQuery, keyword));
    }
}
