package com.campus.emergency.modules.auth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.emergency.modules.auth.entity.AuthRefreshToken;
import com.campus.emergency.modules.auth.mapper.AuthRefreshTokenMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
public class AuthRefreshTokenCleanupTask {

    private final AuthRefreshTokenMapper authRefreshTokenMapper;

    public AuthRefreshTokenCleanupTask(AuthRefreshTokenMapper authRefreshTokenMapper) {
        this.authRefreshTokenMapper = authRefreshTokenMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    @Scheduled(cron = "${security.jwt.cleanup-cron:0 0/30 * * * ?}")
    public void scheduledCleanup() {
        int removed = cleanupNow();
        if (removed > 0) {
            log.info("Auth refresh token scheduled cleanup finished, removed={}", removed);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public int cleanupNow() {
        LocalDateTime now = LocalDateTime.now();
        return authRefreshTokenMapper.delete(new QueryWrapper<AuthRefreshToken>()
                .lt("expire_at", now)
                .or()
                .eq("revoked", 1));
    }
}
