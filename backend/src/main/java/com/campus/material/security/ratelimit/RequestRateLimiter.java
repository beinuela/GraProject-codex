package com.campus.material.security.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RequestRateLimiter {

    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(Duration.ofHours(1))
            .build();

    public boolean tryConsume(String key, long capacity, long windowSeconds) {
        Bucket bucket = cache.get(key, ignored -> newBucket(capacity, windowSeconds));
        return bucket.tryConsume(1);
    }

    private Bucket newBucket(long capacity, long windowSeconds) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(capacity, Duration.ofSeconds(windowSeconds)));
        return Bucket.builder().addLimit(limit).build();
    }
}
