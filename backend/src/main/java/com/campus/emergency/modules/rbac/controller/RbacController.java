package com.campus.emergency.modules.rbac.controller;

import com.campus.emergency.common.ApiResponse;
import com.campus.emergency.modules.rbac.entity.SysDept;
import com.campus.emergency.modules.rbac.entity.SysRole;
import com.campus.emergency.modules.rbac.entity.SysUser;
import com.campus.emergency.modules.rbac.service.RbacService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('ADMIN','APPROVER')")
    public ApiResponse<List<SysRole>> roles() {
        return ApiResponse.ok(rbacService.listRoles());
    }

    @PostMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SysRole> saveRole(@RequestBody SysRole role) {
        return ApiResponse.ok(rbacService.saveRole(role));
    }

    @GetMapping("/depts")
    @PreAuthorize("hasAnyRole('ADMIN','APPROVER','DEPT_USER')")
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
