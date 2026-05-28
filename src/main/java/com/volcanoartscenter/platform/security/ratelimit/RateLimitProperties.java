package com.volcanoartscenter.platform.security.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "platform.rate-limits")
public class RateLimitProperties {

    private boolean enabled = true;
    private String redisNamespace = "volcano:rl";
    private Map<String, Policy> policies = new LinkedHashMap<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getRedisNamespace() { return redisNamespace; }
    public void setRedisNamespace(String redisNamespace) { this.redisNamespace = redisNamespace; }

    public Map<String, Policy> getPolicies() { return policies; }
    public void setPolicies(Map<String, Policy> policies) { this.policies = policies; }

    public static class Policy {
        private long capacity;
        private long refillTokens;
        private Duration refillPeriod;

        public long getCapacity() { return capacity; }
        public void setCapacity(long capacity) { this.capacity = capacity; }

        public long getRefillTokens() { return refillTokens; }
        public void setRefillTokens(long refillTokens) { this.refillTokens = refillTokens; }

        public Duration getRefillPeriod() { return refillPeriod; }
        public void setRefillPeriod(Duration refillPeriod) { this.refillPeriod = refillPeriod; }
    }
}
