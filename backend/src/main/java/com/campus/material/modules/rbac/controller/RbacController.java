package com.campus.material.modules.rbac.controller;

import com.campus.material.common.ApiResponse;
import com.campus.material.modules.rbac.entity.SysDept;
import com.campus.material.modules.rbac.entity.SysRole;
import com.campus.material.modules.rbac.entity.SysUser;
import com.campus.material.modules.rbac.service.RbacService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rbac")
public class RbacController {

    private final RbacService rbacService;

    public RbacController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<SysUser>> users() {
        return ApiResponse.ok(rbacService.listUsers());
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SysUser> saveUser(@RequestBody SysUser user) {
        return ApiResponse.ok(rbacService.saveUser(user));
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        rbacService.deleteUser(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/users/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> request) {
        rbacService.resetPassword(id, request == null ? null : request.get("password"));
        return ApiResponse.ok(null);
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('ADMIN','APPROVER','WAREHOUSE_ADMIN')")
    public ApiResponse<List<SysRole>> roles() {
        return ApiResponse.ok(rbacService.listRoles());
    }

    @PostMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SysRole> saveRole(@RequestBody SysRole role) {
        return ApiResponse.ok(rbacService.saveRole(role));
    }

    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteRole(@PathVariable Long id) {
        rbacService.deleteRole(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/depts")
    @PreAuthorize("hasAnyRole('ADMIN','APPROVER','DEPT_USER','USER')")
    public ApiResponse<List<SysDept>> depts() {
        return ApiResponse.ok(rbacService.listDepts());
    }

    @PostMapping("/depts")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SysDept> saveDept(@RequestBody SysDept dept) {
        return ApiResponse.ok(rbacService.saveDept(dept));
    }

    @DeleteMapping("/depts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteDept(@PathVariable Long id) {
        rbacService.deleteDept(id);
        return ApiResponse.ok(null);
    }
}
