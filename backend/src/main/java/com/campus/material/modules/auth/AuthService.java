package com.campus.material.modules.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.material.common.BizException;
import com.campus.material.monitoring.BusinessMetrics;
import com.campus.material.modules.auth.dto.LoginRequest;
import com.campus.material.modules.auth.dto.LoginResponse;
import com.campus.material.modules.auth.entity.AuthRefreshToken;
import com.campus.material.modules.auth.mapper.AuthRefreshTokenMapper;
import com.campus.material.modules.log.service.LoginLogService;
import com.campus.material.modules.rbac.entity.SysRole;
import com.campus.material.modules.rbac.entity.SysUser;
import com.campus.material.modules.rbac.mapper.SysRoleMapper;
import com.campus.material.modules.rbac.mapper.SysUserMapper;
import com.campus.material.security.AuthUtil;
import com.campus.material.security.JwtProperties;
import com.campus.material.security.JwtTokenProvider;
import com.campus.material.security.LoginUser;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginLogService loginLogService;
    private final AuthRefreshTokenMapper authRefreshTokenMapper;
    private final JwtProperties jwtProperties;
    private final AuthRefreshTokenCleanupTask authRefreshTokenCleanupTask;
    private final BusinessMetrics businessMetrics;

    public AuthService(SysUserMapper userMapper, SysRoleMapper roleMapper, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider, LoginLogService loginLogService,
                       AuthRefreshTokenMapper authRefreshTokenMapper, JwtProperties jwtProperties,
                       AuthRefreshTokenCleanupTask authRefreshTokenCleanupTask,
                       BusinessMetrics businessMetrics) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.loginLogService = loginLogService;
        this.authRefreshTokenMapper = authRefreshTokenMapper;
        this.jwtProperties = jwtProperties;
        this.authRefreshTokenCleanupTask = authRefreshTokenCleanupTask;
        this.businessMetrics = businessMetrics;
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername() == null ? null : request.getUsername().trim();
        HttpServletRequest servletRequest = currentRequest();
        String loginIp = servletRequest == null ? "" : servletRequest.getRemoteAddr();
        String userAgent = servletRequest == null ? "" : defaultString(servletRequest.getHeader("User-Agent"));
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null || !passwordMatched(request.getPassword(), user.getPassword())) {
            businessMetrics.recordLoginFailure();
            loginLogService.record(null, username, loginIp, "FAILURE", userAgent);
            throw new BizException(401, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            businessMetrics.recordLoginFailure();
            loginLogService.record(user.getId(), user.getUsername(), loginIp, "FAILURE", userAgent);
            throw new BizException(403, "账号已被禁用");
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        String roleCode = role == null ? "DEPT_USER" : role.getRoleCode();
        LoginResponse response = issueTokens(user, roleCode);
        businessMetrics.recordLoginSuccess();
        loginLogService.record(user.getId(), user.getUsername(), loginIp, "SUCCESS", userAgent);
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtTokenProvider.parseToken(refreshToken);
        } catch (Exception e) {
            businessMetrics.recordRefreshFailure();
            throw new BizException(401, "refresh token 无效或已过期");
        }
        String tokenType = (String) claims.get("typ");
        if (!"refresh".equals(tokenType)) {
            businessMetrics.recordRefreshFailure();
            throw new BizException(401, "token 类型错误");
        }
        String tokenId = claims.getId();
        if (tokenId == null || tokenId.isBlank()) {
            businessMetrics.recordRefreshFailure();
            throw new BizException(401, "refresh token 无效");
        }
        Long userId = ((Number) claims.get("uid")).longValue();

        AuthRefreshToken stored = authRefreshTokenMapper.selectOne(new LambdaQueryWrapper<AuthRefreshToken>()
                .eq(AuthRefreshToken::getUserId, userId)
                .eq(AuthRefreshToken::getTokenId, tokenId)
                .eq(AuthRefreshToken::getRevoked, 0)
                .last("limit 1"));
        if (stored == null) {
            businessMetrics.recordRefreshFailure();
            throw new BizException(401, "refresh token 已失效");
        }
        if (stored.getExpireAt() == null || stored.getExpireAt().isBefore(LocalDateTime.now())) {
            revokeById(stored.getId());
            businessMetrics.recordRefreshFailure();
            throw new BizException(401, "refresh token 已过期");
        }
        if (!sha256Base64(refreshToken).equals(stored.getTokenHash())) {
            revokeById(stored.getId());
            businessMetrics.recordRefreshFailure();
            throw new BizException(401, "refresh token 校验失败");
        }

        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            revokeById(stored.getId());
            businessMetrics.recordRefreshFailure();
            throw new BizException(403, "账号不可用");
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        String roleCode = role == null ? "DEPT_USER" : role.getRoleCode();

        /**
         * refresh token 采用单次使用策略。
         * 只有在持久化的 token 记录仍然有效且 hash 校验通过时，才会进入轮换流程。
         * 一旦校验通过，先撤销旧 token，再签发新 token，避免旧 refresh token 被重复利用。
         */
        revokeById(stored.getId());
        return issueTokens(user, roleCode);
    }

    private LoginResponse issueTokens(SysUser user, String roleCode) {
        if (!jwtProperties.isMultiDeviceLogin()) {
            revokeAllActiveTokensByUserId(user.getId());
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), roleCode);
        String refreshJti = UUID.randomUUID().toString();
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername(), roleCode, refreshJti);

        AuthRefreshToken entity = new AuthRefreshToken();
        entity.setUserId(user.getId());
        entity.setTokenId(refreshJti);
        entity.setTokenHash(sha256Base64(refreshToken));
        entity.setExpireAt(LocalDateTime.now().plusDays(jwtProperties.getRefreshExpireDays()));
        entity.setRevoked(0);
        authRefreshTokenMapper.insert(entity);

        return LoginResponse.builder()
                .token(accessToken)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessExpireMinutes() * 60)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .roleCode(roleCode)
                .build();
    }

    private void revokeAllActiveTokensByUserId(Long userId) {
        authRefreshTokenMapper.update(null, new LambdaUpdateWrapper<AuthRefreshToken>()
                .eq(AuthRefreshToken::getUserId, userId)
                .eq(AuthRefreshToken::getRevoked, 0)
                .set(AuthRefreshToken::getRevoked, 1));
    }

    private void revokeById(Long id) {
        authRefreshTokenMapper.update(null, new LambdaUpdateWrapper<AuthRefreshToken>()
                .eq(AuthRefreshToken::getId, id)
                .set(AuthRefreshToken::getRevoked, 1));
    }

    private String sha256Base64(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new BizException(500, "token 处理失败");
        }
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

    public void logoutCurrentUser() {
        Long uid = AuthUtil.currentUserId();
        if (uid == null) {
            return;
        }
        authRefreshTokenMapper.update(null, new LambdaUpdateWrapper<AuthRefreshToken>()
                .eq(AuthRefreshToken::getUserId, uid)
                .eq(AuthRefreshToken::getRevoked, 0)
                .set(AuthRefreshToken::getRevoked, 1));
    }

            public Map<String, Object> tokenPolicyOverview() {
            LocalDateTime now = LocalDateTime.now();
            Long total = authRefreshTokenMapper.selectCount(new LambdaQueryWrapper<AuthRefreshToken>());
            Long active = authRefreshTokenMapper.selectCount(new LambdaQueryWrapper<AuthRefreshToken>()
                .eq(AuthRefreshToken::getRevoked, 0)
                .gt(AuthRefreshToken::getExpireAt, now));
            Long revoked = authRefreshTokenMapper.selectCount(new LambdaQueryWrapper<AuthRefreshToken>()
                .eq(AuthRefreshToken::getRevoked, 1));
            Long expired = authRefreshTokenMapper.selectCount(new LambdaQueryWrapper<AuthRefreshToken>()
                .eq(AuthRefreshToken::getRevoked, 0)
                .le(AuthRefreshToken::getExpireAt, now));

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("multiDeviceLogin", jwtProperties.isMultiDeviceLogin());
            map.put("accessExpireMinutes", jwtProperties.getAccessExpireMinutes());
            map.put("refreshExpireDays", jwtProperties.getRefreshExpireDays());
            map.put("cleanupCron", jwtProperties.getCleanupCron());
            map.put("tokenTotal", total == null ? 0 : total);
            map.put("activeTokenCount", active == null ? 0 : active);
            map.put("revokedTokenCount", revoked == null ? 0 : revoked);
            map.put("expiredTokenCount", expired == null ? 0 : expired);
            return map;
            }

            public Map<String, Object> triggerTokenCleanup() {
            int removed = authRefreshTokenCleanupTask.cleanupNow();
            Map<String, Object> result = new HashMap<>();
            result.put("removed", removed);
            result.put("cleanedAt", LocalDateTime.now());
            return result;
            }

    private List<Map<String, String>> buildMenusByRole(String role) {
        List<Map<String, String>> all = new ArrayList<>();
        addMenu(all, "dashboard", "仪表盘", "/dashboard", "首页");
        if ("ADMIN".equals(role)) {
            // 系统管理
            addMenu(all, "users", "用户管理", "/rbac/users", "系统管理");
            addMenu(all, "roles", "角色管理", "/rbac/roles", "系统管理");
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
            addMenu(all, "event", "事件管理", "/event/list", "安全监控");
            addMenu(all, "analytics", "统计分析", "/analytics/charts", "安全监控");
            // 系统工具
            addMenu(all, "oplog", "操作日志", "/log/operation", "系统工具");
            addMenu(all, "loginlog", "登录日志", "/log/login", "系统工具");
            addMenu(all, "notification", "消息通知", "/notification/list", "系统工具");
            addMenu(all, "sysconfig", "系统配置", "/config/list", "系统工具");
            addMenu(all, "securityPolicy", "安全策略", "/security/policy", "系统工具");
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
            addMenu(all, "event", "事件管理", "/event/list", "安全监控");
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

        // 仅支持 BCrypt 加密密码校验
        try {
            return passwordEncoder.matches(raw, stored);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
