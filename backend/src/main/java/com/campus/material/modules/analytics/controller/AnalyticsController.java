package com.campus.material.modules.analytics.controller;

import com.campus.material.common.ApiResponse;
import com.campus.material.modules.analytics.service.AnalyticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER','DEPT_USER','USER','PURCHASER','DISPATCHER')")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.ok(analyticsService.overview());
    }

    @GetMapping("/inventory-ratio")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER','DEPT_USER','USER','PURCHASER','DISPATCHER')")
    public ApiResponse<List<Map<String, Object>>> inventoryRatio() {
        return ApiResponse.ok(analyticsService.inventoryRatio());
    }

    @GetMapping("/inbound-outbound-trend")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER','DEPT_USER','USER','PURCHASER','DISPATCHER')")
    public ApiResponse<List<Map<String, Object>>> inboundOutboundTrend() {
        return ApiResponse.ok(analyticsService.inboundOutboundTrend());
    }

    @GetMapping("/department-ranking")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER','DEPT_USER','USER','PURCHASER','DISPATCHER')")
    public ApiResponse<List<Map<String, Object>>> departmentRanking() {
        return ApiResponse.ok(analyticsService.departmentRanking());
    }

    @GetMapping("/expiry-stats")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER','DEPT_USER','USER','PURCHASER','DISPATCHER')")
    public ApiResponse<List<Map<String, Object>>> expiryStats() {
        return ApiResponse.ok(analyticsService.expiryStats());
    }

    @GetMapping("/warehouse-distribution")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER','DEPT_USER','USER','PURCHASER','DISPATCHER')")
    public ApiResponse<List<Map<String, Object>>> warehouseDistribution() {
        return ApiResponse.ok(analyticsService.warehouseDistribution());
    }

    @GetMapping("/emergency-consumption")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER','DEPT_USER','USER','PURCHASER','DISPATCHER')")
    public ApiResponse<List<Map<String, Object>>> emergencyConsumption() {
        return ApiResponse.ok(analyticsService.emergencyConsumption());
    }
}
