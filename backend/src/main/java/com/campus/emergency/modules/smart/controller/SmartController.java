package com.campus.emergency.modules.smart.controller;

import com.campus.emergency.common.ApiResponse;
import com.campus.emergency.modules.smart.service.SmartService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/smart")
public class SmartController {

    private final SmartService smartService;

    public SmartController(SmartService smartService) {
        this.smartService = smartService;
    }

    @GetMapping("/forecast")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER')")
    public ApiResponse<Map<String, Object>> forecast(@RequestParam Long materialId,
                                                     @RequestParam(defaultValue = "3") int months) {
        return ApiResponse.ok(smartService.forecast(materialId, months));
    }

    @GetMapping("/replenishment-suggestions")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER')")
    public ApiResponse<List<Map<String, Object>>> replenishmentSuggestions(@RequestParam(defaultValue = "7") Integer guaranteeDays) {
        return ApiResponse.ok(smartService.replenishmentSuggestions(guaranteeDays));
    }
}
