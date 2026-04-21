package com.campus.material.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;

@Component
public class AppStartupValidator implements ApplicationRunner {

    private static final Set<String> EXCLUDED_PROFILES = Set.of("test");

    private final JwtProperties jwtProperties;
    private final Environment environment;

    public AppStartupValidator(JwtProperties jwtProperties, Environment environment) {
        this.jwtProperties = jwtProperties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (Arrays.stream(environment.getActiveProfiles()).anyMatch(EXCLUDED_PROFILES::contains)) {
            return;
        }

        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET 未配置，非测试环境启动前必须通过环境变量提供。");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET 长度不足，至少需要 32 字节。");
        }
    }
}
