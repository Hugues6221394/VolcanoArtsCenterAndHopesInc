package com.volcanoartscenter.platform.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RateLimitingService {

    // Simple memory-backed limits (resets on restart), sufficient for DoS protection
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String ip) {
        return buckets.computeIfAbsent(ip, this::newBucket);
    }

    private Bucket newBucket(String ip) {
        // Limit: 30 requests per minute per IP for sensitive endpoints
        Bandwidth limit = Bandwidth.builder().capacity(30).refillGreedy(30, Duration.ofMinutes(1)).build();
        return Bucket.builder().addLimit(limit).build();
    }
}
