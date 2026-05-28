package com.volcanoartscenter.platform.web.internal.api;

import com.volcanoartscenter.platform.shared.exception.NotFoundException;
import com.volcanoartscenter.platform.shared.payment.OrderStatusHistory;
import com.volcanoartscenter.platform.shared.payment.Payment;
import com.volcanoartscenter.platform.shared.payment.PaymentRepository;
import com.volcanoartscenter.platform.shared.service.WebhookProcessingService;
import com.volcanoartscenter.platform.shared.web.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Staff-side payment ops. Currently used to confirm offline payments
 * (bank transfer, cash deposit) — the staff member verifies receipt out-of-band
 * and POSTs here to flip the polymorphic Payment row to CAPTURED, which
 * cascades into the Booking / ShippingOrder transition + notification.
 *
 * <p>Restricted to OPS_MANAGER / SUPER_ADMIN by SecurityConfig {@code /api/v1/ops/**}.
 */
@RestController
@RequestMapping("/api/v1/ops/payments")
@RequiredArgsConstructor
public class StaffPaymentApiController {

    private final PaymentRepository paymentRepository;
    private final WebhookProcessingService webhookProcessingService;

    public record ConfirmPaymentDto(@Valid String note) {}

    @PostMapping("/{paymentId}/confirm")
    public ApiResponse<Map<String, Object>> confirm(@PathVariable Long paymentId,
                                                    @RequestBody(required = false) ConfirmPaymentDto body) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment", paymentId));
        Payment captured = webhookProcessingService.confirmOfflinePayment(
                payment.getGateway(), payment.getGatewayRef(), OrderStatusHistory.Actor.STAFF);
        return ApiResponse.ok(Map.of(
                "paymentId", captured.getId(),
                "gateway", captured.getGateway().name(),
                "status", captured.getStatus().name(),
                "sourceType", captured.getSourceType().name(),
                "sourceId", captured.getSourceId()
        ), body == null || body.note() == null ? "Payment captured." : body.note());
    }
}
