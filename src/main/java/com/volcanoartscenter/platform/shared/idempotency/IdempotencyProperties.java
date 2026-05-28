package com.volcanoartscenter.platform.shared.idempotency;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "platform.idempotency")
public class IdempotencyProperties {

    private boolean enabled = true;
    private int ttlHours = 24;
    private String redisNamespace = "volcano:idem";
    private List<String> protectedPaths = List.of(
            "/api/v1/client/checkout",
            "/api/v1/client/bookings",
            "/api/v1/client/donations",
            "/api/v1/partner/bookings"
    );

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getTtlHours() { return ttlHours; }
    public void setTtlHours(int ttlHours) { this.ttlHours = ttlHours; }

    public String getRedisNamespace() { return redisNamespace; }
    public void setRedisNamespace(String redisNamespace) { this.redisNamespace = redisNamespace; }

    public List<String> getProtectedPaths() { return protectedPaths; }
    public void setProtectedPaths(List<String> protectedPaths) { this.protectedPaths = protectedPaths; }
}
