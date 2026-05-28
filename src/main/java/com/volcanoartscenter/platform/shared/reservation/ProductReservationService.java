package com.volcanoartscenter.platform.shared.reservation;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Race-safe unique-artwork reservation via Redis (PRD §4.1).
 * Atomic acquire (SET NX EX) and atomic release (Lua compare-and-delete).
 * TTL handles automatic expiry — no sweeper required.
 */
@Service
public class ProductReservationService {

    public static final Duration DEFAULT_TTL = Duration.ofMinutes(15);
    private static final String NAMESPACE = "volcano:reservation:product:";

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redis;

    public ProductReservationService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Attempt to reserve a unique product for the given holder.
     * Returns true if reserved, false if already held by someone else.
     */
    public boolean tryReserve(Long productId, String holderKey) {
        return tryReserve(productId, holderKey, DEFAULT_TTL);
    }

    public boolean tryReserve(Long productId, String holderKey, Duration ttl) {
        String key = key(productId);
        String existing = redis.opsForValue().get(key);
        if (existing != null && existing.equals(holderKey)) {
            redis.expire(key, ttl);
            return true;
        }
        Boolean ok = redis.opsForValue().setIfAbsent(key, holderKey, ttl);
        return Boolean.TRUE.equals(ok);
    }

    /**
     * Release the reservation only if the caller is still the holder.
     */
    public void release(Long productId, String holderKey) {
        Long deleted = redis.execute(RELEASE_SCRIPT, List.of(key(productId)), holderKey);
        // returns 1 if released, 0 otherwise — we don't need to act on either
    }

    /**
     * Force-release regardless of holder. Used by checkout-success handlers
     * that have authoritatively transitioned the artwork to SOLD.
     */
    public void forceRelease(Long productId) {
        redis.delete(key(productId));
    }

    public boolean isReserved(Long productId) {
        return Boolean.TRUE.equals(redis.hasKey(key(productId)));
    }

    public Optional<String> currentHolder(Long productId) {
        return Optional.ofNullable(redis.opsForValue().get(key(productId)));
    }

    /**
     * Seconds remaining on the reservation; empty if unreserved.
     */
    public Optional<Long> remainingSeconds(Long productId) {
        Long secs = redis.getExpire(key(productId), TimeUnit.SECONDS);
        if (secs == null || secs < 0) return Optional.empty();
        return Optional.of(secs);
    }

    private static String key(Long productId) {
        return NAMESPACE + productId;
    }
}
