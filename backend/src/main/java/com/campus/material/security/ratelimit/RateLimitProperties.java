package com.campus.material.security.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "security.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private boolean trustForwardHeaders = false;
    private long loginCapacity = 5;
    private long loginWindowSeconds = 60;
    private long refreshCapacity = 10;
    private long refreshWindowSeconds = 60;
    private long highRiskCapacity = 20;
    private long highRiskWindowSeconds = 60;
}
