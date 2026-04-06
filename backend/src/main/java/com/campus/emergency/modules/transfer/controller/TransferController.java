package com.campus.emergency.modules.transfer.controller;

import com.campus.emergency.common.ApiResponse;
import com.campus.emergency.modules.transfer.dto.TransferCreateRequest;
import com.campus.emergency.modules.transfer.entity.TransferOrder;
import com.campus.emergency.modules.transfer.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transfer")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER')")
    public ApiResponse<List<TransferOrder>> list() {
        return ApiResponse.ok(transferService.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER')")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(transferService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN')")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody TransferCreateRequest request) {
        return ApiResponse.ok(transferService.create(request));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN')")
    public ApiResponse<Void> submit(@PathVariable Long id) {
        transferService.submit(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','APPROVER')")
    public ApiResponse<Void> approve(@PathVariable Long id, @RequestParam(required = false) String remark) {
        transferService.approve(id, remark);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','APPROVER')")
    public ApiResponse<Void> reject(@PathVariable Long id, @RequestParam(required = false) String remark) {
        transferService.reject(id, remark);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN')")
    public ApiResponse<Void> execute(@PathVariable Long id) {
        transferService.execute(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN')")
    public ApiResponse<Void> receive(@PathVariable Long id) {
        transferService.receive(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/recommend")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN')")
    public ApiResponse<List<Map<String, Object>>> recommend(
            @RequestParam String targetCampus,
            @RequestParam Long materialId,
            @RequestParam Integer qty) {
        return ApiResponse.ok(transferService.recommendTransfer(targetCampus, materialId, qty));
    }
}
