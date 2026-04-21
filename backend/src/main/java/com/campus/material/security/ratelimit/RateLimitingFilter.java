package com.campus.material.security.ratelimit;

import com.campus.material.common.ApiResponse;
import com.campus.material.common.ErrorCode;
import com.campus.material.security.LoginUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int TOO_MANY_REQUESTS_STATUS = 429;
    private static final Set<String> HIGH_RISK_PATHS = Set.of(
            "/api/warning/scan",
            "/api/auth/logout",
            "/api/auth/token-cleanup"
    );

    private final RequestRateLimiter requestRateLimiter;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(RequestRateLimiter requestRateLimiter, RateLimitProperties properties, ObjectMapper objectMapper) {
        this.requestRateLimiter = requestRateLimiter;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.isEnabled() || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        HttpServletRequest effectiveRequest = request;
        if (requiresBodyCaching(request)) {
            effectiveRequest = new CachedBodyHttpServletRequest(request);
        }

        if (isLoginRequest(effectiveRequest)) {
            String username = extractField(effectiveRequest, "username");
            String key = "login:" + resolveClientIp(effectiveRequest) + ":" + username;
            if (!requestRateLimiter.tryConsume(key, properties.getLoginCapacity(), properties.getLoginWindowSeconds())) {
                writeRateLimited(response, "登录请求过于频繁，请稍后重试");
                return;
            }
        } else if (isRefreshRequest(effectiveRequest)) {
            String refreshToken = extractField(effectiveRequest, "refreshToken");
            String key = "refresh:" + resolveClientIp(effectiveRequest) + ":" + hashToken(refreshToken);
            if (!requestRateLimiter.tryConsume(key, properties.getRefreshCapacity(), properties.getRefreshWindowSeconds())) {
                writeRateLimited(response, "刷新令牌请求过于频繁，请稍后重试");
                return;
            }
        } else if (isHighRiskRequest(effectiveRequest)) {
            String subject = resolvePrincipalKey();
            String key = "high-risk:" + effectiveRequest.getRequestURI() + ":" + subject;
            if (!requestRateLimiter.tryConsume(key, properties.getHighRiskCapacity(), properties.getHighRiskWindowSeconds())) {
                writeRateLimited(response, "高风险操作触发限流，请稍后重试");
                return;
            }
        }

        filterChain.doFilter(effectiveRequest, response);
    }

    private boolean requiresBodyCaching(HttpServletRequest request) {
        return isLoginRequest(request) || isRefreshRequest(request);
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && "/api/auth/login".equals(request.getRequestURI());
    }

    private boolean isRefreshRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && "/api/auth/refresh".equals(request.getRequestURI());
    }

    private boolean isHighRiskRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && HIGH_RISK_PATHS.contains(request.getRequestURI());
    }

    private String extractField(HttpServletRequest request, String fieldName) {
        if (!(request instanceof CachedBodyHttpServletRequest cachedRequest) || cachedRequest.getCachedBody().length == 0) {
            return "anonymous";
        }
        try {
            JsonNode node = objectMapper.readTree(cachedRequest.getCachedBody());
            JsonNode field = node.get(fieldName);
            return field == null || field.asText().isBlank() ? "anonymous" : field.asText().trim();
        } catch (Exception ignored) {
            return "anonymous";
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (properties.isTrustForwardHeaders()) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private String resolvePrincipalKey() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return "ip-only";
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUser loginUser && loginUser.getUserId() != null) {
            return "user:" + loginUser.getUserId();
        }
        return "anonymous";
    }

    private String hashToken(String token) {
        if (token == null || token.isBlank()) {
            return "anonymous";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ignored) {
            return "anonymous";
        }
    }

    private void writeRateLimited(HttpServletResponse response, String message) throws IOException {
        response.setStatus(TOO_MANY_REQUESTS_STATUS);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.fail(ErrorCode.TOO_MANY_REQUESTS.getCode(), message)
        ));
    }
}
