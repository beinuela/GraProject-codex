package com.campus.material.modules.warning.controller;

import com.campus.material.common.ApiResponse;
import com.campus.material.common.RemarkRequest;
import com.campus.material.modules.warning.entity.WarningRecord;
import com.campus.material.modules.warning.service.WarningService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warning")
public class WarningController {

    private final WarningService warningService;

    public WarningController(WarningService warningService) {
        this.warningService = warningService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER')")
    public ApiResponse<List<WarningRecord>> list(@RequestParam(required = false) String type,
                                                 @RequestParam(required = false) String status) {
        return ApiResponse.ok(warningService.list(type, status));
    }

    @PostMapping("/scan")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN')")
    public ApiResponse<Void> scan() {
        warningService.scan();
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/handle")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER')")
    public ApiResponse<Void> handle(@PathVariable Long id,
                                    @RequestParam(required = false) String remark,
                                    @RequestBody(required = false) RemarkRequest body) {
        warningService.handle(id, resolveRemark(remark, body));
        return ApiResponse.ok(null);
    }

    private String resolveRemark(String remark, RemarkRequest body) {
        if (body != null && body.getRemark() != null && !body.getRemark().isBlank()) {
            return body.getRemark();
        }
        return remark;
    }
}
