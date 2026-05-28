package com.volcanoartscenter.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volcanoartscenter.platform.security.ratelimit.RateLimitPolicyResolver;
import com.volcanoartscenter.platform.security.ratelimit.RateLimitPolicyResolver.Resolved;
import com.volcanoartscenter.platform.security.ratelimit.RateLimitPolicyResolver.Scope;
import com.volcanoartscenter.platform.security.ratelimit.RedisRateLimiter;
import com.volcanoartscenter.platform.shared.web.api.ApiError;
import com.volcanoartscenter.platform.shared.web.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitPolicyResolver resolver;
    private final RedisRateLimiter limiter;
    private final ObjectMapper mapper;

    public RateLimitInterceptor(RateLimitPolicyResolver resolver, RedisRateLimiter limiter, ObjectMapper mapper) {
        this.resolver = resolver;
        this.limiter = limiter;
        this.mapper = mapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Resolved resolved = resolver.resolve(request);
        if (resolved == null) {
            return true;
        }
        String scopeKey = resolveScopeKey(request, resolved.scope());
        RedisRateLimiter.Decision decision = limiter.tryConsume(resolved.policy(), scopeKey);

        if (decision.allowed()) {
            response.addHeader("X-RateLimit-Policy", resolved.policy());
            response.addHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
            return true;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.addHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
        response.addHeader("X-RateLimit-Policy", resolved.policy());

        if (wantsJson(request)) {
            response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            response.getOutputStream().write(mapper.writeValueAsBytes(
                    ApiResponse.failure(ApiError.of("RATE_LIMITED",
                            "Too many requests. Try again in " + decision.retryAfterSeconds() + " seconds.",
                            Map.of("policy", resolved.policy(), "retryAfterSeconds", decision.retryAfterSeconds())))
            ));
        } else {
            response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
            response.getWriter().write("Too many requests. Please try again in " + decision.retryAfterSeconds() + " seconds.");
        }
        return false;
    }

    private String resolveScopeKey(HttpServletRequest request, Scope scope) {
        if (scope == Scope.USER) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                return "u:" + auth.getName();
            }
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return "ip:" + ip.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private boolean wantsJson(HttpServletRequest request) {
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return request.getRequestURI().startsWith("/api/")
                || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE));
    }
}
