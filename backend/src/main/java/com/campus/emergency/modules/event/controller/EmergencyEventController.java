package com.campus.emergency.modules.event.controller;

import com.campus.emergency.common.ApiResponse;
import com.campus.emergency.modules.event.entity.EmergencyEvent;
import com.campus.emergency.modules.event.service.EmergencyEventService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/event")
public class EmergencyEventController {

    private final EmergencyEventService eventService;

    public EmergencyEventController(EmergencyEventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER','DEPT_USER')")
    public ApiResponse<List<EmergencyEvent>> list(@RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String eventType) {
        return ApiResponse.ok(eventService.list(status, eventType));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER','DEPT_USER')")
    public ApiResponse<EmergencyEvent> detail(@PathVariable Long id) {
        return ApiResponse.ok(eventService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER','DEPT_USER')")
    public ApiResponse<Void> create(@RequestBody EmergencyEvent event) {
        eventService.create(event);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/handle")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN')")
    public ApiResponse<Void> handle(@PathVariable Long id, @RequestParam Long handlerId,
                                     @RequestParam String handleResult) {
        eventService.handle(id, handlerId, handleResult);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER')")
    public ApiResponse<Void> close(@PathVariable Long id, @RequestParam String handleResult) {
        eventService.close(id, handleResult);
        return ApiResponse.ok(null);
    }
}
