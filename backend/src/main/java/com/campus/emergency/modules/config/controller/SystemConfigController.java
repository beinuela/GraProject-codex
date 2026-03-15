package com.campus.emergency.modules.config.controller;

import com.campus.emergency.common.ApiResponse;
import com.campus.emergency.modules.config.entity.SystemConfig;
import com.campus.emergency.modules.config.service.SystemConfigService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/config")
public class SystemConfigController {

    private final SystemConfigService configService;

    public SystemConfigController(SystemConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<List<SystemConfig>> list(@RequestParam(required = false) String group) {
        return ApiResponse.ok(configService.list(group));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<Void> save(@RequestBody SystemConfig config) {
        configService.save(config);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        configService.delete(id);
        return ApiResponse.ok(null);
    }
}
