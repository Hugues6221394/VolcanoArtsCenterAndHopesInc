package com.volcanoartscenter.platform.shared.idempotency;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class IdempotencyStore {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final IdempotencyProperties props;

    public IdempotencyStore(StringRedisTemplate redis, ObjectMapper mapper, IdempotencyProperties props) {
        this.redis = redis;
        this.mapper = mapper;
        this.props = props;
    }

    public boolean acquire(String key) {
        Boolean ok = redis.opsForValue().setIfAbsent(
                fullKey(key),
                "PROCESSING",
                Duration.ofMinutes(2));
        return Boolean.TRUE.equals(ok);
    }

    public void release(String key) {
        redis.delete(fullKey(key));
    }

    public void store(String key, CachedResponse response) {
        try {
            redis.opsForValue().set(
                    fullKey(key),
                    mapper.writeValueAsString(response),
                    Duration.ofHours(props.getTtlHours()));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize idempotent response", ex);
        }
    }

    public Optional<CachedResponse> read(String key) {
        String raw = redis.opsForValue().get(fullKey(key));
        if (raw == null || "PROCESSING".equals(raw)) {
            return Optional.empty();
        }
        try {
            return Optional.of(mapper.readValue(raw, CachedResponse.class));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize idempotent response", ex);
        }
    }

    public boolean isProcessing(String key) {
        return "PROCESSING".equals(redis.opsForValue().get(fullKey(key)));
    }

    private String fullKey(String key) {
        return props.getRedisNamespace() + ":" + key;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CachedResponse(int status, String contentType, String body) {}
}
