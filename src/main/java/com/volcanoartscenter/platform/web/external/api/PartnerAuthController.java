package com.volcanoartscenter.platform.web.external.api;

import com.volcanoartscenter.platform.security.jwt.PartnerJwtService;
import com.volcanoartscenter.platform.shared.exception.PlatformException;
import com.volcanoartscenter.platform.shared.model.User;
import com.volcanoartscenter.platform.shared.repository.UserRepository;
import com.volcanoartscenter.platform.shared.web.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

/**
 * Partner / tour-operator JWT issuance.
 *
 * <p>POST /api/v1/partner/auth/token — exchange email + password for a short-lived
 * RS256 access token. Caller must already hold the {@code TOUR_OPERATOR} role
 * (or {@code OPS_MANAGER} / {@code SUPER_ADMIN} for staff-side automation).
 */
@RestController
@RequestMapping("/api/v1/partner/auth")
@RequiredArgsConstructor
public class PartnerAuthController {

    private final AuthenticationManager authenticationManager;
    private final PartnerJwtService partnerJwtService;
    private final UserRepository userRepository;

    public record TokenRequest(@NotBlank @Email String email,
                               @NotBlank String password) {}

    @PostMapping("/token")
    public ApiResponse<PartnerJwtService.IssuedToken> issueToken(@Valid @RequestBody TokenRequest body) {
        String email = body.email().trim().toLowerCase(Locale.ROOT);
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, body.password()));
        } catch (BadCredentialsException ex) {
            throw new PlatformException("INVALID_CREDENTIALS",
                    "Invalid email or password.", HttpStatus.UNAUTHORIZED);
        } catch (AuthenticationException ex) {
            throw new PlatformException("AUTHENTICATION_FAILED",
                    ex.getMessage() == null ? "Authentication failed." : ex.getMessage(),
                    HttpStatus.UNAUTHORIZED);
        }

        boolean partnerEligible = auth.getAuthorities().stream().anyMatch(a -> {
            String role = a.getAuthority();
            return "ROLE_TOUR_OPERATOR".equals(role)
                    || "ROLE_OPS_MANAGER".equals(role)
                    || "ROLE_SUPER_ADMIN".equals(role);
        });
        if (!partnerEligible) {
            throw new PlatformException("FORBIDDEN_ROLE",
                    "Account is not authorised for partner API access.",
                    HttpStatus.FORBIDDEN);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new PlatformException("USER_NOT_FOUND",
                        "Authenticated user record missing.", HttpStatus.UNAUTHORIZED));
        return ApiResponse.ok(partnerJwtService.issueAccessToken(user),
                "Access token issued. Send as 'Authorization: Bearer <token>' on /api/v1/partner/**");
    }
}
