package com.campus.emergency.modules.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.emergency.common.BizException;
import com.campus.emergency.modules.auth.dto.LoginRequest;
import com.campus.emergency.modules.auth.dto.LoginResponse;
import com.campus.emergency.modules.log.service.LoginLogService;
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
    private final LoginLogService loginLogService;

    public AuthService(SysUserMapper userMapper, SysRoleMapper roleMapper, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider, LoginLogService loginLogService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.loginLogService = loginLogService;
    }

    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername() == null ? null : request.getUsername().trim();
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null || !passwordMatched(request.getPassword(), user.getPassword())) {
            // 记录登录失败日志
            loginLogService.record(null, username, "", "0", "");
            throw new BizException(401, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            loginLogService.record(user.getId(), user.getUsername(), "", "0", "");
            throw new BizException(403, "账号已被禁用");
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        String roleCode = role == null ? "DEPT_USER" : role.getRoleCode();
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), roleCode);
        // 记录登录成功日志
        loginLogService.record(user.getId(), user.getUsername(), "", "1", "");
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
        addMenu(all, "dashboard", "仪表盘", "/dashboard", "首页");
        if ("ADMIN".equals(role)) {
            // 系统管理
            addMenu(all, "users", "用户管理", "/rbac/users", "系统管理");
            addMenu(all, "depts", "部门管理", "/rbac/depts", "系统管理");
            // 基础数据
            addMenu(all, "campus", "校区管理", "/campus/list", "基础数据");
            addMenu(all, "category", "物资分类", "/material/category", "基础数据");
            addMenu(all, "material", "物资信息", "/material/info", "基础数据");
            addMenu(all, "supplier", "供应商管理", "/supplier/list", "基础数据");
            // 仓储管理
            addMenu(all, "warehouse", "仓库管理", "/warehouse/list", "仓储管理");
            addMenu(all, "location", "库位管理", "/warehouse/location", "仓储管理");
            addMenu(all, "inventory", "库存查询", "/inventory/list", "仓储管理");
            addMenu(all, "stockin", "入库管理", "/inventory/stock-in", "仓储管理");
            addMenu(all, "stockout", "出库管理", "/inventory/stock-out", "仓储管理");
            // 业务操作
            addMenu(all, "apply", "申领审批", "/apply/list", "业务操作");
            addMenu(all, "transfer", "调拨管理", "/transfer/list", "业务操作");
            // 安全监控
            addMenu(all, "warning", "预警管理", "/warning/list", "安全监控");
            addMenu(all, "event", "应急事件", "/event/list", "安全监控");
            addMenu(all, "analytics", "统计分析", "/analytics/charts", "安全监控");
            // 系统工具
            addMenu(all, "oplog", "操作日志", "/log/operation", "系统工具");
            addMenu(all, "loginlog", "登录日志", "/log/login", "系统工具");
            addMenu(all, "notification", "消息通知", "/notification/list", "系统工具");
            addMenu(all, "sysconfig", "系统配置", "/config/list", "系统工具");
            return all;
        }
        if ("WAREHOUSE_ADMIN".equals(role)) {
            addMenu(all, "material", "物资信息", "/material/info", "基础数据");
            addMenu(all, "supplier", "供应商管理", "/supplier/list", "基础数据");
            addMenu(all, "warehouse", "仓库管理", "/warehouse/list", "仓储管理");
            addMenu(all, "location", "库位管理", "/warehouse/location", "仓储管理");
            addMenu(all, "inventory", "库存查询", "/inventory/list", "仓储管理");
            addMenu(all, "stockin", "入库管理", "/inventory/stock-in", "仓储管理");
            addMenu(all, "stockout", "出库管理", "/inventory/stock-out", "仓储管理");
            addMenu(all, "transfer", "调拨管理", "/transfer/list", "业务操作");
            addMenu(all, "warning", "预警管理", "/warning/list", "安全监控");
            addMenu(all, "event", "应急事件", "/event/list", "安全监控");
            addMenu(all, "analytics", "统计分析", "/analytics/charts", "安全监控");
            addMenu(all, "notification", "消息通知", "/notification/list", "系统工具");
            return all;
        }
        if ("APPROVER".equals(role)) {
            addMenu(all, "apply", "申领审批", "/apply/list", "业务操作");
            addMenu(all, "transfer", "调拨管理", "/transfer/list", "业务操作");
            addMenu(all, "warning", "预警管理", "/warning/list", "安全监控");
            addMenu(all, "analytics", "统计分析", "/analytics/charts", "安全监控");
            return all;
        }
        addMenu(all, "apply", "申领审批", "/apply/list", "业务操作");
        addMenu(all, "analytics", "统计分析", "/analytics/charts", "安全监控");
        return all;
    }

    private void addMenu(List<Map<String, String>> menus, String key, String title, String path, String group) {
        Map<String, String> map = new HashMap<>();
        map.put("key", key);
        map.put("title", title);
        map.put("path", path);
        map.put("group", group);
        menus.add(map);
    }

    private boolean passwordMatched(String raw, String encodedOrPlain) {
        if (raw == null || encodedOrPlain == null) {
            return false;
        }

        String stored = encodedOrPlain.trim();

        // 兼容 DelegatingPasswordEncoder 存储格式: {bcrypt}$2a$...
        if (stored.startsWith("{bcrypt}")) {
            stored = stored.substring("{bcrypt}".length());
        }

        // 优先按加密密码匹配（支持 BCrypt 及后续可扩展编码器）
        try {
            if (passwordEncoder.matches(raw, stored)) {
                return true;
            }
        } catch (IllegalArgumentException ignored) {
            // 非 BCrypt 格式时回退到明文兼容
        }

        // 兼容演示环境中的明文密码
        return raw.equals(stored) || raw.equals(encodedOrPlain);
    }
}
