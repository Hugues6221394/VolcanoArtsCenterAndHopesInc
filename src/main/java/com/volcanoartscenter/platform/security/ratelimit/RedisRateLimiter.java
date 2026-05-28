package com.volcanoartscenter.platform.security.ratelimit;

import com.volcanoartscenter.platform.security.ratelimit.RateLimitProperties.Policy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Fixed-window rate limiter backed by Redis atomic INCR + EXPIRE.
 * Atomically increments a counter for the (policy, scope) key and returns the new value.
 * If the counter exceeds the policy capacity within the window, the request is rejected.
 */
@Component
@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RedisRateLimiter {

    private static final DefaultRedisScript<Long> SCRIPT;
    static {
        SCRIPT = new DefaultRedisScript<>(
                "local v = redis.call('INCR', KEYS[1]) " +
                "if v == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end " +
                "return v",
                Long.class);
    }

    private final StringRedisTemplate redis;
    private final RateLimitProperties props;

    public RedisRateLimiter(StringRedisTemplate redis, RateLimitProperties props) {
        this.redis = redis;
        this.props = props;
    }

    public Decision tryConsume(String policyName, String scopeKey) {
        if (!props.isEnabled()) {
            return Decision.allowed(Long.MAX_VALUE);
        }
        Policy policy = props.getPolicies().get(policyName);
        if (policy == null) {
            return Decision.allowed(Long.MAX_VALUE);
        }

        String key = props.getRedisNamespace() + ":" + policyName + ":" + scopeKey;
        long windowMs = policy.getRefillPeriod() == null ? 60_000L : policy.getRefillPeriod().toMillis();

        Long current = redis.execute(SCRIPT, List.of(key), String.valueOf(windowMs));
        long count = current == null ? 0L : current;

        if (count > policy.getCapacity()) {
            Long ttlMs = redis.getExpire(key, java.util.concurrent.TimeUnit.MILLISECONDS);
            long retryAfterSec = ttlMs == null || ttlMs < 0 ? policy.getRefillPeriod().toSeconds() : (ttlMs + 999) / 1000;
            return Decision.denied(retryAfterSec);
        }
        return Decision.allowed(Math.max(0, policy.getCapacity() - count));
    }

    public record Decision(boolean allowed, long remaining, long retryAfterSeconds) {
        public static Decision allowed(long remaining) {
            return new Decision(true, remaining, 0);
        }
        public static Decision denied(long retryAfterSeconds) {
            return new Decision(false, 0, retryAfterSeconds);
        }
    }
}
