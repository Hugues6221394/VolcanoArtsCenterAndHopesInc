package com.volcanoartscenter.platform.shared.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class RateLimitedException extends PlatformException {
    public RateLimitedException(String policy, long retryAfterSeconds) {
        super("RATE_LIMITED",
                "Too many requests. Try again in " + retryAfterSeconds + " seconds.",
                HttpStatus.TOO_MANY_REQUESTS,
                Map.of("policy", policy, "retryAfterSeconds", retryAfterSeconds));
    }
}
