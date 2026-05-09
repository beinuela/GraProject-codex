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
    private static final String PROD_PROFILE = "prod";

    private final JwtProperties jwtProperties;
    private final Environment environment;

    public AppStartupValidator(JwtProperties jwtProperties, Environment environment) {
        this.jwtProperties = jwtProperties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String[] activeProfiles = environment.getActiveProfiles();
        if (Arrays.stream(activeProfiles).anyMatch(EXCLUDED_PROFILES::contains)) {
            return;
        }

        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET 未配置，非测试环境启动前必须通过环境变量提供。");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET 长度不足，至少需要 32 字节。");
        }

        if (Arrays.asList(activeProfiles).contains(PROD_PROFILE)) {
            validateProdDatasource();
        }
    }

    private void validateProdDatasource() {
        String url = environment.getProperty("spring.datasource.url");
        String username = environment.getProperty("spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password");

        if (url == null || url.isBlank()) {
            throw new IllegalStateException("prod 环境必须配置 DB_URL。");
        }
        if (!url.startsWith("jdbc:mysql:")) {
            throw new IllegalStateException("prod 环境必须使用 MySQL 数据源，当前 spring.datasource.url=" + url);
        }
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("prod 环境必须配置 DB_USERNAME。");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("prod 环境必须配置 DB_PASSWORD。");
        }
    }
}
