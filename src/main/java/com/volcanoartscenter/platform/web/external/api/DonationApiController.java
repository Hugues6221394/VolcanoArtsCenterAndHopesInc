package com.volcanoartscenter.platform.web.external.api;

import com.volcanoartscenter.platform.shared.donation.DonationReceiptService;
import com.volcanoartscenter.platform.shared.donation.DonationService;
import com.volcanoartscenter.platform.shared.exception.NotFoundException;
import com.volcanoartscenter.platform.shared.exception.PlatformException;
import com.volcanoartscenter.platform.shared.model.Donation;
import com.volcanoartscenter.platform.shared.model.User;
import com.volcanoartscenter.platform.shared.repository.UserRepository;
import com.volcanoartscenter.platform.shared.web.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/public/donations")
@RequiredArgsConstructor
public class DonationApiController {

    private final DonationService donationService;
    private final DonationReceiptService donationReceiptService;
    private final UserRepository userRepository;

    public record CreateDonationDto(
            Long campaignId,
            @NotNull @DecimalMin("1.0") BigDecimal amount,
            String currency,
            Donation.DonationPurpose purpose,
            String donorName,
            @Email String donorEmail,
            String donorCountry,
            String message,
            Boolean anonymous,
            Boolean recurring,
            Donation.RecurringFrequency recurringFrequency) {}

    @PostMapping
    public ApiResponse<DonationService.DonationResult> donate(Authentication authentication,
                                                              @Valid @RequestBody CreateDonationDto body) {
        User user = optionalUser(authentication);
        DonationService.DonationRequest req = new DonationService.DonationRequest(
                body.campaignId(), body.amount(), body.currency(), body.purpose(),
                body.donorName(), body.donorEmail(), body.donorCountry(), body.message(),
                body.anonymous(), body.recurring(), body.recurringFrequency());
        return ApiResponse.ok(donationService.recordDonation(user, req),
                "Donation recorded. Confirm card to capture the first charge.");
    }

    @GetMapping("/{reference}/receipt.pdf")
    public ResponseEntity<byte[]> receipt(@PathVariable String reference) {
        Donation donation = donationService.findByReference(reference)
                .orElseThrow(() -> new NotFoundException("Donation", reference));
        byte[] pdf = donationReceiptService.generate(donation);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", reference + "-receipt.pdf");
        headers.setContentLength(pdf.length);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    private User optionalUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new PlatformException(
                        "USER_NOT_FOUND", "Authenticated user record missing.", HttpStatus.UNAUTHORIZED));
    }
}
