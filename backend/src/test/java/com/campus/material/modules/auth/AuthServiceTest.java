package com.campus.material.modules.auth;

import com.campus.material.common.BizException;
import com.campus.material.monitoring.BusinessMetrics;
import com.campus.material.modules.auth.dto.LoginRequest;
import com.campus.material.modules.auth.dto.LoginResponse;
import com.campus.material.modules.auth.mapper.AuthRefreshTokenMapper;
import com.campus.material.modules.log.service.LoginLogService;
import com.campus.material.modules.rbac.entity.SysRole;
import com.campus.material.modules.rbac.entity.SysUser;
import com.campus.material.modules.rbac.mapper.SysRoleMapper;
import com.campus.material.modules.rbac.mapper.SysUserMapper;
import com.campus.material.security.JwtProperties;
import com.campus.material.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysRoleMapper roleMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private LoginLogService loginLogService;

    @Mock
    private AuthRefreshTokenMapper authRefreshTokenMapper;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private AuthRefreshTokenCleanupTask authRefreshTokenCleanupTask;

    @Mock
    private BusinessMetrics businessMetrics;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("Abc@123456");

        SysUser mockUser = new SysUser();
        mockUser.setId(1L);
        mockUser.setUsername("admin");
        mockUser.setPassword("encodedPassword");
        mockUser.setRoleId(1L);
        mockUser.setStatus(1);

        SysRole mockRole = new SysRole();
        mockRole.setRoleCode("ADMIN");

        when(sysUserMapper.selectOne(any())).thenReturn(mockUser);
        when(roleMapper.selectById(1L)).thenReturn(mockRole);
        when(passwordEncoder.matches("Abc@123456", "encodedPassword")).thenReturn(true);
        when(jwtProperties.getAccessExpireMinutes()).thenReturn(30);
        when(jwtProperties.getRefreshExpireDays()).thenReturn(7);
        when(jwtProperties.isMultiDeviceLogin()).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(eq(1L), eq("admin"), eq("ADMIN"))).thenReturn("mock.access.token");
        when(jwtTokenProvider.generateRefreshToken(eq(1L), eq("admin"), eq("ADMIN"), anyString())).thenReturn("mock.refresh.token");

        LoginResponse result = authService.login(request);

        assertNotNull(result);
        assertEquals("mock.access.token", result.getToken());
        assertEquals("mock.refresh.token", result.getRefreshToken());
        assertEquals(1L, result.getUserId());
        assertEquals("ADMIN", result.getRoleCode());
    }

    @Test
    void testLoginWrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("WrongPassword");

        SysUser mockUser = new SysUser();
        mockUser.setUsername("admin");
        mockUser.setPassword("encodedPassword");
        mockUser.setStatus(1);

        when(sysUserMapper.selectOne(any())).thenReturn(mockUser);
        when(passwordEncoder.matches("WrongPassword", "encodedPassword")).thenReturn(false);

        BizException thrown = assertThrows(BizException.class, () -> authService.login(request));
        assertTrue(thrown.getMessage().contains("用户名或密码错误"));
    }

    @Test
    void testLoginUserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("Abc@123456");

        when(sysUserMapper.selectOne(any())).thenReturn(null);

        BizException thrown = assertThrows(BizException.class, () -> authService.login(request));
        assertTrue(thrown.getMessage().contains("用户名或密码错误"));
    }

    @Test
    void testLoginUserDisabled() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("Abc@123456");

        SysUser mockUser = new SysUser();
        mockUser.setUsername("admin");
        mockUser.setPassword("encodedPassword");
        mockUser.setStatus(0); // Disabled

        when(sysUserMapper.selectOne(any())).thenReturn(mockUser);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        BizException thrown = assertThrows(BizException.class, () -> authService.login(request));
        assertTrue(thrown.getMessage().contains("禁用"));
    }

    @Test
    void testRefreshInvalidToken() {
        when(jwtTokenProvider.parseToken("bad-token")).thenThrow(new RuntimeException("invalid"));

        BizException thrown = assertThrows(BizException.class, () -> authService.refresh("bad-token"));
        assertEquals(401, thrown.getCode());
    }

    @Test
    void testRefreshWrongTokenType() {
        Claims claims = mock(Claims.class);
        when(jwtTokenProvider.parseToken("token")).thenReturn(claims);
        when(claims.get("typ")).thenReturn("access");

        BizException thrown = assertThrows(BizException.class, () -> authService.refresh("token"));
        assertEquals(401, thrown.getCode());
        verify(authRefreshTokenMapper, never()).selectOne(any());
    }

    @Test
    void testRefreshTokenIdMissing() {
        Claims claims = mock(Claims.class);
        when(jwtTokenProvider.parseToken("token")).thenReturn(claims);
        when(claims.get("typ")).thenReturn("refresh");
        when(claims.getId()).thenReturn(" ");

        BizException thrown = assertThrows(BizException.class, () -> authService.refresh("token"));
        assertEquals(401, thrown.getCode());
        verify(authRefreshTokenMapper, never()).selectOne(any());
    }

    @Test
    void testRefreshRevokedOrMissingInStore() {
        Claims claims = mock(Claims.class);
        when(jwtTokenProvider.parseToken("token")).thenReturn(claims);
        when(claims.get("typ")).thenReturn("refresh");
        when(claims.getId()).thenReturn("jti-1");
        when(claims.get("uid")).thenReturn(1L);
        when(authRefreshTokenMapper.selectOne(any())).thenReturn(null);

        BizException thrown = assertThrows(BizException.class, () -> authService.refresh("token"));
        assertEquals(401, thrown.getCode());
    }

}
