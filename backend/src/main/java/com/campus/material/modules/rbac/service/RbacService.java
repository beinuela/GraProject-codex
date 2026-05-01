package com.campus.material.modules.rbac.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.material.common.BizException;
import com.campus.material.modules.log.service.OperationLogService;
import com.campus.material.modules.rbac.entity.SysDept;
import com.campus.material.modules.rbac.entity.SysRole;
import com.campus.material.modules.rbac.entity.SysUser;
import com.campus.material.modules.rbac.mapper.SysDeptMapper;
import com.campus.material.modules.rbac.mapper.SysRoleMapper;
import com.campus.material.modules.rbac.mapper.SysUserMapper;
import com.campus.material.security.AuthUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RbacService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService operationLogService;

    public RbacService(SysUserMapper userMapper, SysRoleMapper roleMapper, SysDeptMapper deptMapper, PasswordEncoder passwordEncoder,
                       OperationLogService operationLogService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.deptMapper = deptMapper;
        this.passwordEncoder = passwordEncoder;
        this.operationLogService = operationLogService;
    }

    public List<SysUser> listUsers() {
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>().orderByDesc(SysUser::getId));
    }

    public SysUser saveUser(SysUser user) {
        validateUser(user);
        if (user.getId() == null) {
            if (user.getPassword() == null || user.getPassword().isBlank()) {
                throw new BizException(400, "密码不能为空");
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            userMapper.insert(user);
            operationLogService.log(AuthUtil.currentUserId(), "RBAC", "CREATE_USER", user.getUsername());
        } else {
            SysUser old = userMapper.selectById(user.getId());
            if (old == null) {
                throw new BizException("用户不存在");
            }
            if (user.getPassword() == null || user.getPassword().isBlank()) {
                user.setPassword(old.getPassword());
            } else {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
            userMapper.updateById(user);
            operationLogService.log(AuthUtil.currentUserId(), "RBAC", "UPDATE_USER", user.getUsername());
        }
        return userMapper.selectById(user.getId());
    }

    public void deleteUser(Long id) {
        userMapper.deleteById(id);
        operationLogService.log(AuthUtil.currentUserId(), "RBAC", "DELETE_USER", String.valueOf(id));
    }

    public List<SysRole> listRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
    }

    public SysRole saveRole(SysRole role) {
        validateRole(role);
        if (role.getId() == null) {
            roleMapper.insert(role);
            operationLogService.log(AuthUtil.currentUserId(), "RBAC", "CREATE_ROLE", role.getRoleCode());
        } else {
            roleMapper.updateById(role);
            operationLogService.log(AuthUtil.currentUserId(), "RBAC", "UPDATE_ROLE", role.getRoleCode());
        }
        return roleMapper.selectById(role.getId());
    }

    public List<SysDept> listDepts() {
        return deptMapper.selectList(new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getId));
    }

    public SysDept saveDept(SysDept dept) {
        if (dept.getId() == null) {
            deptMapper.insert(dept);
            operationLogService.log(AuthUtil.currentUserId(), "RBAC", "CREATE_DEPT", dept.getDeptName());
        } else {
            deptMapper.updateById(dept);
            operationLogService.log(AuthUtil.currentUserId(), "RBAC", "UPDATE_DEPT", dept.getDeptName());
        }
        return deptMapper.selectById(dept.getId());
    }

    public void deleteDept(Long id) {
        deptMapper.deleteById(id);
        operationLogService.log(AuthUtil.currentUserId(), "RBAC", "DELETE_DEPT", String.valueOf(id));
    }

    private void validateUser(SysUser user) {
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new BizException(400, "用户名不能为空");
        }
        String username = user.getUsername().trim();
        SysUser duplicated = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .ne(user.getId() != null, SysUser::getId, user.getId())
                .last("limit 1"));
        if (duplicated != null) {
            throw new BizException(409, "用户账号已存在，请更换账号");
        }
        user.setUsername(username);
        if (user.getRealName() != null) {
            user.setRealName(user.getRealName().trim());
        }
    }

    private void validateRole(SysRole role) {
        if (role.getRoleCode() == null || role.getRoleCode().isBlank()) {
            throw new BizException(400, "角色编码不能为空");
        }
        String roleCode = role.getRoleCode().trim();
        SysRole duplicated = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode)
                .ne(role.getId() != null, SysRole::getId, role.getId())
                .last("limit 1"));
        if (duplicated != null) {
            throw new BizException(409, "角色编码已存在，请勿重复添加");
        }
        role.setRoleCode(roleCode);
        if (role.getRoleName() != null) {
            role.setRoleName(role.getRoleName().trim());
        }
    }
}
