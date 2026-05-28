package com.volcanoartscenter.platform.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RateLimitPolicyResolver {

    private record Rule(String policy, String pathPrefix, Scope scope) {}

    public enum Scope { IP, USER }

    public record Resolved(String policy, Scope scope) {}

    private static final List<Rule> RULES = List.of(
            new Rule("login", "/login", Scope.IP),
            new Rule("login", "/api/v1/auth/login", Scope.IP),
            new Rule("register", "/register", Scope.IP),
            new Rule("register", "/api/v1/auth/register", Scope.IP),
            new Rule("register", "/talent/register", Scope.IP),
            new Rule("register", "/tour-operators/register", Scope.IP),
            new Rule("password-reset", "/password-reset", Scope.IP),
            new Rule("password-reset", "/api/v1/auth/password-reset", Scope.IP),
            new Rule("contact-form", "/contact", Scope.IP),
            new Rule("talent-form", "/talent/apply", Scope.IP),
            new Rule("talent-form", "/api/v1/public/talent-applications", Scope.IP),
            new Rule("payment-init", "/api/v1/client/checkout", Scope.USER),
            new Rule("payment-init", "/api/v1/client/donations", Scope.USER),
            new Rule("authenticated-api", "/api/v1/client", Scope.USER),
            new Rule("authenticated-api", "/api/v1/partner", Scope.USER),
            new Rule("authenticated-api", "/api/v1/talent", Scope.USER),
            new Rule("authenticated-api", "/api/v1/cms", Scope.USER),
            new Rule("authenticated-api", "/api/v1/ops", Scope.USER),
            new Rule("authenticated-api", "/api/v1/admin", Scope.USER),
            new Rule("public-api", "/api/v1/public", Scope.IP),
            new Rule("public-api", "/api/public", Scope.IP)
    );

    public Resolved resolve(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (Rule rule : RULES) {
            if (path.startsWith(rule.pathPrefix())) {
                return new Resolved(rule.policy(), rule.scope());
            }
        }
        return null;
    }
}
