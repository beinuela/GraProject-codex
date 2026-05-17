package com.campus.material.modules.delivery.controller;

import com.campus.material.common.ApiResponse;
import com.campus.material.common.PageQuery;
import com.campus.material.common.PageResult;
import com.campus.material.modules.delivery.entity.DeliveryTask;
import com.campus.material.modules.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/delivery")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','DISPATCHER','APPROVER','DEPT_USER','USER')")
    public ApiResponse<PageResult<DeliveryTask>> list(@Valid PageQuery pageQuery,
                                                      @RequestParam(required = false) String status) {
        return ApiResponse.ok(deliveryService.list(pageQuery, status));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','DISPATCHER')")
    public ApiResponse<DeliveryTask> create(@RequestBody DeliveryTask task) {
        return ApiResponse.ok(deliveryService.create(task));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ApiResponse<Void> assign(@PathVariable Long id, @RequestBody Map<String, Long> request) {
        deliveryService.assign(id, request == null ? null : request.get("dispatcherId"));
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ApiResponse<Void> start(@PathVariable Long id) {
        deliveryService.start(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/sign")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','DEPT_USER','USER')")
    public ApiResponse<Void> sign(@PathVariable Long id) {
        deliveryService.sign(id);
        return ApiResponse.ok(null);
    }
}
