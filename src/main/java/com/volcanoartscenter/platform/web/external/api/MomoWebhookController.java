package com.volcanoartscenter.platform.web.external.api;

import com.volcanoartscenter.platform.shared.service.WebhookProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * MTN MoMo callback receiver. MoMo POSTs status notifications to the callback
 * URL configured at request-to-pay time. The body shape (per MoMo docs):
 * <pre>{ "financialTransactionId":"...", "externalId":"...",
 *        "amount":"...", "currency":"...", "status":"SUCCESSFUL|FAILED|PENDING",
 *        "reason":{ "code":"...", "message":"..." } }</pre>
 *
 * <p>The {@code X-Reference-Id} header (when present) is the same UUID we sent
 * on /requesttopay and equals our {@code Payment.gatewayRef}.
 */
@RestController
@RequestMapping("/api/v1/webhooks/momo")
@RequiredArgsConstructor
@Slf4j
public class MomoWebhookController {

    private final WebhookProcessingService webhookProcessingService;

    @PostMapping
    public ResponseEntity<String> handleCallback(
            @RequestHeader(value = "X-Reference-Id", required = false) String headerRef,
            @RequestBody(required = false) Map<String, Object> body) {
        if (body == null) body = Map.of();
        String externalId = String.valueOf(body.getOrDefault("externalId", ""));
        String referenceId = headerRef != null && !headerRef.isBlank()
                ? headerRef
                : String.valueOf(body.getOrDefault("referenceId", ""));
        String status = String.valueOf(body.getOrDefault("status", ""));
        String reason = extractReason(body.get("reason"));
        String eventId = "momo-" + (referenceId.isBlank() ? externalId : referenceId);

        log.info("MoMo callback received: ref={} status={} eventId={}", referenceId, status, eventId);

        if (referenceId.isBlank()) {
            log.warn("MoMo callback dropped — no reference id (externalId={})", externalId);
            return ResponseEntity.badRequest().body("missing reference id");
        }
        try {
            webhookProcessingService.processMomoCallback(eventId, referenceId, status, reason);
            return ResponseEntity.ok("ok");
        } catch (Exception ex) {
            log.error("MoMo callback processing failed for ref {}", referenceId, ex);
            return ResponseEntity.status(500).body("processing failed");
        }
    }

    private String extractReason(Object reason) {
        if (reason instanceof Map<?, ?> map) {
            Object msg = map.get("message");
            if (msg != null) return String.valueOf(msg);
            Object code = map.get("code");
            if (code != null) return String.valueOf(code);
        }
        return reason == null ? null : String.valueOf(reason);
    }
}
