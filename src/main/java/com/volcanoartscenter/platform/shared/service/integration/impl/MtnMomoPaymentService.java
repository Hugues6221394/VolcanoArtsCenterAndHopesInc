package com.volcanoartscenter.platform.shared.service.integration.impl;

import com.volcanoartscenter.platform.shared.service.integration.PaymentGatewayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * MTN MoMo Collections (Request-To-Pay) gateway.
 *
 * <p>Auth flow: POST {@code /collection/token/} with HTTP Basic
 * (apiUserId:apiKey) + {@code Ocp-Apim-Subscription-Key} → receive Bearer
 * access token (typ. ~3600s TTL). The token is cached in-process and refreshed
 * 5 minutes before expiry.
 *
 * <p>Initialize: POST {@code /collection/v1_0/requesttopay} with the cached
 * Bearer + a fresh {@code X-Reference-Id} (the gateway reference we persist
 * to {@code payments.gateway_ref}). Status check: GET the same path.
 */
@Service
@Slf4j
public class MtnMomoPaymentService implements PaymentGatewayService {

    private final RestClient restClient;
    private final String baseUrl;
    private final String apiUserId;
    private final String apiKey;
    private final String subscriptionKey;
    private final String targetEnvironment;
    private final String currency;
    private final String callbackUrl;

    private volatile String cachedToken;
    private volatile Instant cachedTokenExpiresAt = Instant.EPOCH;

    public MtnMomoPaymentService(
            @Value("${platform.integrations.mtn.base-url:}") String baseUrl,
            @Value("${platform.integrations.mtn.api-user-id:}") String apiUserId,
            @Value("${platform.integrations.mtn.api-key:}") String apiKey,
            @Value("${platform.integrations.mtn.subscription-key:}") String subscriptionKey,
            @Value("${platform.integrations.mtn.target-environment:sandbox}") String targetEnvironment,
            @Value("${platform.integrations.mtn.currency:EUR}") String currency,
            @Value("${platform.integrations.mtn.callback-url:}") String callbackUrl) {
        this.restClient = RestClient.builder().build();
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.apiUserId = apiUserId;
        this.apiKey = apiKey;
        this.subscriptionKey = subscriptionKey;
        this.targetEnvironment = targetEnvironment;
        this.currency = (currency == null || currency.isBlank()) ? "EUR" : currency;
        this.callbackUrl = callbackUrl;
    }

    @Override
    public String provider() { return "MTN_MOMO"; }

    public boolean isConfigured() {
        return !isBlank(baseUrl) && !isBlank(apiUserId) && !isBlank(apiKey) && !isBlank(subscriptionKey);
    }

    @Override
    public PaymentResult initialize(String reference, BigDecimal amount, String currencyOverride, Map<String, String> metadata) {
        ensureConfigured();
        String payerMsisdn = metadata == null ? null : metadata.get("msisdn");
        if (payerMsisdn == null || payerMsisdn.isBlank()) {
            throw new IllegalArgumentException("MTN MoMo requires 'msisdn' (payer phone) in metadata");
        }
        String gatewayRef = UUID.randomUUID().toString();
        String token = obtainAccessToken();
        String resolvedCurrency = (currencyOverride == null || currencyOverride.isBlank()) ? currency : currencyOverride;
        try {
            RestClient.RequestBodySpec req = restClient.post()
                    .uri(baseUrl + "/collection/v1_0/requesttopay")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Reference-Id", gatewayRef)
                    .header("X-Target-Environment", targetEnvironment)
                    .header("Ocp-Apim-Subscription-Key", subscriptionKey)
                    .header("Authorization", "Bearer " + token);
            if (callbackUrl != null && !callbackUrl.isBlank()) {
                req = req.header("X-Callback-Url", callbackUrl);
            }
            req.body(Map.of(
                            "amount", amount.toPlainString(),
                            "currency", resolvedCurrency,
                            "externalId", reference,
                            "payer", Map.of("partyIdType", "MSISDN", "partyId", payerMsisdn),
                            "payerMessage", "Volcano Arts Center payment",
                            "payeeNote", "Reference " + reference
                    ))
                    .retrieve()
                    .toBodilessEntity();
            return new PaymentResult(true, gatewayRef, "MTN MoMo request created");
        } catch (RestClientResponseException ex) {
            log.warn("MoMo requestToPay failed: {} {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new IllegalStateException("MTN MoMo initialization failed: " + ex.getResponseBodyAsString(), ex);
        }
    }

    @Override
    public PaymentResult verify(String externalReference) {
        ensureConfigured();
        String token = obtainAccessToken();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(baseUrl + "/collection/v1_0/requesttopay/{reference}", externalReference)
                    .header("X-Target-Environment", targetEnvironment)
                    .header("Ocp-Apim-Subscription-Key", subscriptionKey)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(Map.class);
            String status = response == null ? "UNKNOWN" : String.valueOf(response.get("status"));
            boolean success = "SUCCESSFUL".equalsIgnoreCase(status);
            return new PaymentResult(success, externalReference, "MTN status: " + status);
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("MTN MoMo verification failed: " + ex.getResponseBodyAsString(), ex);
        }
    }

    private String obtainAccessToken() {
        Instant now = Instant.now();
        if (cachedToken != null && now.isBefore(cachedTokenExpiresAt.minusSeconds(60))) {
            return cachedToken;
        }
        synchronized (this) {
            if (cachedToken != null && Instant.now().isBefore(cachedTokenExpiresAt.minusSeconds(60))) {
                return cachedToken;
            }
            String basic = Base64.getEncoder().encodeToString(
                    (apiUserId + ":" + apiKey).getBytes(StandardCharsets.UTF_8));
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> response = restClient.post()
                        .uri(baseUrl + "/collection/token/")
                        .header("Authorization", "Basic " + basic)
                        .header("Ocp-Apim-Subscription-Key", subscriptionKey)
                        .retrieve()
                        .body(Map.class);
                if (response == null || response.get("access_token") == null) {
                    throw new IllegalStateException("MoMo /collection/token/ returned no access_token");
                }
                this.cachedToken = String.valueOf(response.get("access_token"));
                long ttl = response.get("expires_in") instanceof Number n ? n.longValue() : 3600L;
                this.cachedTokenExpiresAt = Instant.now().plusSeconds(ttl);
                return cachedToken;
            } catch (RestClientResponseException ex) {
                throw new IllegalStateException("MoMo token fetch failed: " + ex.getResponseBodyAsString(), ex);
            }
        }
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException("MTN MoMo integration is not configured");
        }
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String trimTrailingSlash(String s) {
        return (s != null && s.endsWith("/")) ? s.substring(0, s.length() - 1) : s;
    }
}
