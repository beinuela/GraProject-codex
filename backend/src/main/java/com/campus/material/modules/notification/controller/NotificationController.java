package com.campus.material.modules.notification.controller;

import com.campus.material.common.ApiResponse;
import com.campus.material.common.PageQuery;
import com.campus.material.common.PageResult;
import com.campus.material.modules.notification.entity.Notification;
import com.campus.material.modules.notification.service.NotificationService;
import com.campus.material.security.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<Notification>> list(@Valid PageQuery pageQuery) {
        return ApiResponse.ok(notificationService.listByUser(AuthUtil.currentUserId(), pageQuery));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Long> unreadCount() {
        return ApiResponse.ok(notificationService.countUnread(AuthUtil.currentUserId()));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id, AuthUtil.currentUserId());
        return ApiResponse.ok(null);
    }

    @PostMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markAllRead() {
        notificationService.markAllRead(AuthUtil.currentUserId());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        notificationService.delete(id, AuthUtil.currentUserId());
        return ApiResponse.ok(null);
    }
}
