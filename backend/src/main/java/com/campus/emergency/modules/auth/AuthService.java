package com.campus.emergency.modules.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.emergency.common.BizException;
import com.campus.emergency.modules.auth.dto.LoginRequest;
import com.campus.emergency.modules.auth.dto.LoginResponse;
import com.campus.emergency.modules.rbac.entity.SysRole;
import com.campus.emergency.modules.rbac.entity.SysUser;
import com.campus.emergency.modules.rbac.mapper.SysRoleMapper;
import com.campus.emergency.modules.rbac.mapper.SysUserMapper;
import com.campus.emergency.security.AuthUtil;
import com.campus.emergency.security.JwtTokenProvider;
import com.campus.emergency.security.LoginUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(SysUserMapper userMapper, SysRoleMapper roleMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));
        if (user == null || !passwordMatched(request.getPassword(), user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(403, "账号已禁用");
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        String roleCode = role == null ? "DEPT_USER" : role.getRoleCode();
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), roleCode);
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .roleCode(roleCode)
                .build();
    }

    public Map<String, Object> me() {
        LoginUser current = AuthUtil.currentUser();
        if (current == null) {
            throw new BizException(401, "未登录");
        }
        SysUser user = userMapper.selectById(current.getUserId());
        SysRole role = roleMapper.selectById(user.getRoleId());
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("realName", user.getRealName());
        map.put("deptId", user.getDeptId());
        map.put("roleCode", role == null ? "DEPT_USER" : role.getRoleCode());
        return map;
    }

    public List<Map<String, String>> menus() {
        LoginUser current = AuthUtil.currentUser();
        if (current == null) {
            throw new BizException(401, "未登录");
        }
        return buildMenusByRole(current.getRoleCode());
    }

    private List<Map<String, String>> buildMenusByRole(String role) {
        List<Map<String, String>> all = new ArrayList<>();
        addMenu(all, "dashboard", "首页仪表盘", "/dashboard");
        if ("ADMIN".equals(role)) {
            addMenu(all, "users", "用户管理", "/rbac/users");
            addMenu(all, "depts", "部门管理", "/rbac/depts");
            addMenu(all, "category", "物资分类", "/material/category");
            addMenu(all, "material", "物资信息", "/material/info");
            addMenu(all, "warehouse", "仓库管理", "/warehouse/list");
            addMenu(all, "inventory", "库存查询", "/inventory/list");
            addMenu(all, "stockin", "入库管理", "/inventory/stock-in");
            addMenu(all, "stockout", "出库管理", "/inventory/stock-out");
            addMenu(all, "apply", "申请审批", "/apply/list");
            addMenu(all, "transfer", "调拨管理", "/transfer/list");
            addMenu(all, "warning", "预警中心", "/warning/list");
            addMenu(all, "analytics", "数据分析", "/analytics/charts");
            return all;
        }
        if ("WAREHOUSE_ADMIN".equals(role)) {
            addMenu(all, "material", "物资信息", "/material/info");
            addMenu(all, "warehouse", "仓库管理", "/warehouse/list");
            addMenu(all, "inventory", "库存查询", "/inventory/list");
            addMenu(all, "stockin", "入库管理", "/inventory/stock-in");
            addMenu(all, "stockout", "出库管理", "/inventory/stock-out");
            addMenu(all, "transfer", "调拨管理", "/transfer/list");
            addMenu(all, "warning", "预警中心", "/warning/list");
            addMenu(all, "analytics", "数据分析", "/analytics/charts");
            return all;
        }
        if ("APPROVER".equals(role)) {
            addMenu(all, "apply", "申请审批", "/apply/list");
            addMenu(all, "transfer", "调拨管理", "/transfer/list");
            addMenu(all, "warning", "预警中心", "/warning/list");
            addMenu(all, "analytics", "数据分析", "/analytics/charts");
            return all;
        }
        addMenu(all, "apply", "申请审批", "/apply/list");
        addMenu(all, "analytics", "数据分析", "/analytics/charts");
        return all;
    }

    private void addMenu(List<Map<String, String>> menus, String key, String title, String path) {
        Map<String, String> map = new HashMap<>();
        map.put("key", key);
        map.put("title", title);
        map.put("path", path);
        menus.add(map);
    }

    private boolean passwordMatched(String raw, String encodedOrPlain) {
        if (encodedOrPlain == null) {
            return false;
        }
        if (encodedOrPlain.startsWith("$2a$") || encodedOrPlain.startsWith("$2b$") || encodedOrPlain.startsWith("$2y$")) {
            return passwordEncoder.matches(raw, encodedOrPlain);
        }
        return raw.equals(encodedOrPlain);
    }
}
