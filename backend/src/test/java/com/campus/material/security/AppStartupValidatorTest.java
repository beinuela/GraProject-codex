package com.campus.material.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppStartupValidatorTest {

    @Test
    void prodProfileRequiresMysqlCredentials() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("12345678901234567890123456789012");
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:mysql://127.0.0.1:3306/campus_material");
        environment.setActiveProfiles("prod");

        AppStartupValidator validator = new AppStartupValidator(properties, environment);

        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }

    @Test
    void prodProfileAcceptsMysqlConfiguration() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("12345678901234567890123456789012");
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:mysql://127.0.0.1:3306/campus_material")
                .withProperty("spring.datasource.username", "campus_user")
                .withProperty("spring.datasource.password", "campus_password");
        environment.setActiveProfiles("prod");

        AppStartupValidator validator = new AppStartupValidator(properties, environment);

        assertDoesNotThrow(() -> validator.run(null));
    }
}
