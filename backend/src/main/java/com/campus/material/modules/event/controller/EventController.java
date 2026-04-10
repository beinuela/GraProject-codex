package com.campus.material.modules.event.controller;

import com.campus.material.common.ApiResponse;
import com.campus.material.modules.event.entity.EventRecord;
import com.campus.material.modules.event.service.EventService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/event")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER','DEPT_USER')")
    public ApiResponse<List<EventRecord>> list(@RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String eventType) {
        return ApiResponse.ok(eventService.list(status, eventType));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER','DEPT_USER')")
    public ApiResponse<EventRecord> detail(@PathVariable Long id) {
        return ApiResponse.ok(eventService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER','DEPT_USER')")
    public ApiResponse<Void> create(@RequestBody EventRecord event) {
        eventService.create(event);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/handle")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN')")
    public ApiResponse<Void> handle(@PathVariable Long id, @RequestBody(required = false) java.util.Map<String, String> body) {
        String handleResult = body != null ? body.get("handleResult") : "";
        eventService.handle(id, handleResult);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_ADMIN','APPROVER')")
    public ApiResponse<Void> close(@PathVariable Long id, @RequestBody(required = false) java.util.Map<String, String> body) {
        String handleResult = body != null ? body.get("handleResult") : "";
        eventService.close(id, handleResult);
        return ApiResponse.ok(null);
    }
}
