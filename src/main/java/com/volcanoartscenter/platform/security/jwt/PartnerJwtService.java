package com.volcanoartscenter.platform.security.jwt;

import com.volcanoartscenter.platform.shared.model.Role;
import com.volcanoartscenter.platform.shared.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Issues short-lived RS256 JWTs for partner / tour-operator API access.
 * Scope is encoded in the {@code roles} claim ("ROLE_TOUR_OPERATOR", ...) and
 * read back by Spring Security's resource-server JWT converter.
 */
@Service
@RequiredArgsConstructor
public class PartnerJwtService {

    private final JwtEncoder partnerJwtEncoder;

    @Value("${platform.security.jwt.issuer:https://volcanoartscenter.rw}")
    private String issuer;

    @Value("${platform.security.jwt.audience:volcano-partner-api}")
    private String audience;

    @Value("${platform.security.jwt.access-token-ttl-minutes:60}")
    private long accessTtlMinutes;

    public IssuedToken issueAccessToken(User user) {
        Instant now = Instant.now();
        Set<String> roleNames = user.getRoles() == null
                ? Set.of()
                : user.getRoles().stream().map(Role::getName).map(n -> "ROLE_" + n).collect(Collectors.toSet());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(accessTtlMinutes)))
                .subject(user.getEmail())
                .audience(List.of(audience))
                .id(java.util.UUID.randomUUID().toString())
                .claim("uid", user.getId())
                .claim("roles", roleNames)
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String token = partnerJwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, accessTtlMinutes * 60L, "Bearer");
    }

    public record IssuedToken(String accessToken, long expiresInSeconds, String tokenType) {}
}
