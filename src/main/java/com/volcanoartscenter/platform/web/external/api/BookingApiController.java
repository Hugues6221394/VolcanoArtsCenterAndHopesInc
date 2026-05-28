package com.volcanoartscenter.platform.web.external.api;

import com.volcanoartscenter.platform.shared.exception.PlatformException;
import com.volcanoartscenter.platform.shared.model.Booking;
import com.volcanoartscenter.platform.shared.model.User;
import com.volcanoartscenter.platform.shared.payment.BookingService;
import com.volcanoartscenter.platform.shared.payment.PaymentGateway;
import com.volcanoartscenter.platform.shared.repository.UserRepository;
import com.volcanoartscenter.platform.shared.web.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/client/bookings")
@RequiredArgsConstructor
public class BookingApiController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    public record CreateBookingDto(
            @NotNull @Positive Long experienceId,
            @NotNull LocalDate preferredDate,
            LocalDate alternativeDate,
            @NotNull @Positive Integer groupSize,
            String preferredLanguage,
            String specialRequests,
            String guestName,
            @Email String guestEmail,
            String guestPhone,
            String guestCountry,
            @NotNull PaymentGateway paymentMethod,
            String payerMsisdn) {}

    public record CancelBookingDto(@NotBlank String reason) {}

    @PostMapping
    public ApiResponse<BookingService.BookingResult> create(Authentication authentication,
                                                            @Valid @RequestBody CreateBookingDto body) {
        User user = currentUser(authentication);
        BookingService.BookingRequest req = new BookingService.BookingRequest(
                body.experienceId(), body.preferredDate(), body.alternativeDate(),
                body.groupSize(), body.preferredLanguage(), body.specialRequests(),
                body.guestName(), body.guestEmail(), body.guestPhone(), body.guestCountry(),
                body.paymentMethod(), body.payerMsisdn());
        return ApiResponse.ok(bookingService.createBooking(user, req),
                "Booking created. Complete the payment step to confirm.");
    }

    @PostMapping("/{ref}/cancel")
    public ApiResponse<Map<String, Object>> cancel(Authentication authentication,
                                                    @PathVariable("ref") String ref,
                                                    @Valid @RequestBody CancelBookingDto body) {
        User user = currentUser(authentication);
        Booking cancelled = bookingService.cancelBooking(user, ref, body.reason());
        return ApiResponse.ok(Map.of(
                "bookingRef", cancelled.getBookingReference(),
                "status", cancelled.getStatus().name(),
                "cancelledAt", cancelled.getCancelledAt() == null ? "" : cancelled.getCancelledAt().toString()
        ), "Booking cancelled.");
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
