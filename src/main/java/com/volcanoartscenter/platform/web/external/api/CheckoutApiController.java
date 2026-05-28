package com.volcanoartscenter.platform.web.external.api;

import com.volcanoartscenter.platform.shared.exception.PlatformException;
import com.volcanoartscenter.platform.shared.model.User;
import com.volcanoartscenter.platform.shared.payment.CheckoutService;
import com.volcanoartscenter.platform.shared.repository.UserRepository;
import com.volcanoartscenter.platform.shared.web.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/client/checkout")
@RequiredArgsConstructor
public class CheckoutApiController {

    private final CheckoutService checkoutService;
    private final UserRepository userRepository;

    public record CheckoutRequestDto(
            @NotBlank String recipientName,
            @NotBlank @Email String recipientEmail,
            String recipientPhone,
            @NotBlank String addressLine1,
            String addressLine2,
            @NotBlank String city,
            String state,
            String postalCode,
            @NotBlank String country) {}

    @PostMapping
    public ApiResponse<CheckoutService.CheckoutResult> startCheckout(
            Authentication authentication,
            @Valid @RequestBody CheckoutRequestDto body) {
        User user = currentUser(authentication);
        CheckoutService.CheckoutRequest request = new CheckoutService.CheckoutRequest(
                body.recipientName(), body.recipientEmail(), body.recipientPhone(),
                body.addressLine1(), body.addressLine2(), body.city(),
                body.state(), body.postalCode(), body.country());
        CheckoutService.CheckoutResult result = checkoutService.startCardCheckout(user, request);
        return ApiResponse.ok(result, "Payment intent created. Confirm card on the client to complete the order.");
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new PlatformException("UNAUTHENTICATED", "Authentication required.", HttpStatus.UNAUTHORIZED);
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new PlatformException(
                        "USER_NOT_FOUND", "Authenticated user record missing.", HttpStatus.UNAUTHORIZED));
    }
}
