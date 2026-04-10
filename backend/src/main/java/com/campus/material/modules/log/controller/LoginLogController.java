package com.campus.material.modules.log.controller;

import com.campus.material.common.ApiResponse;
import com.campus.material.modules.log.entity.LoginLog;
import com.campus.material.modules.log.service.LoginLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/login-log")
public class LoginLogController {

    private final LoginLogService loginLogService;

    public LoginLogController(LoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<LoginLog>> list() {
        return ApiResponse.ok(loginLogService.list());
    }
}
