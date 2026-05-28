package com.volcanoartscenter.platform.shared.service.integration.impl;

import com.volcanoartscenter.platform.shared.service.integration.PaymentGatewayService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class AirtelMoneyPaymentService implements PaymentGatewayService {
    private final RestClient restClient;
    private final String baseUrl;
    private final String clientId;
    private final String clientSecret;

    public AirtelMoneyPaymentService(@Value("${platform.integrations.airtel.base-url:}") String baseUrl,
                                     @Value("${platform.integrations.airtel.client-id:}") String clientId,
                                     @Value("${platform.integrations.airtel.client-secret:}") String clientSecret) {
        this.restClient = RestClient.builder().build();
        this.baseUrl = baseUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public String provider() { return "AIRTEL_MONEY"; }

    @Override
    public PaymentResult initialize(String reference, BigDecimal amount, String currency, Map<String, String> metadata) {
        ensureConfigured();
        String msisdn = metadata == null ? null : metadata.get("msisdn");
        if (msisdn == null || msisdn.isBlank()) {
            throw new IllegalArgumentException("Airtel Money requires msisdn in metadata");
        }
        String externalReference = "airtel_" + java.util.UUID.randomUUID();
        try {
            restClient.post()
                    .uri(baseUrl + "/merchant/v1/payments/")
                    .header("X-Country", "RW")
                    .header("X-Currency", currency)
                    .header("Authorization", "Basic " + basicToken(clientId, clientSecret))
                    .body(Map.of(
                            "reference", externalReference,
                            "subscriber", Map.of("country", "RW", "currency", currency, "msisdn", msisdn),
                            "transaction", Map.of("amount", amount.toPlainString(), "id", reference)
                    ))
                    .retrieve()
                    .toBodilessEntity();
            return new PaymentResult(true, externalReference, "Airtel request created");
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("Airtel Money initialization failed: " + ex.getResponseBodyAsString(), ex);
        }
    }

    @Override
    public PaymentResult verify(String externalReference) {
        ensureConfigured();
        try {
            Map<?, ?> response = restClient.get()
                    .uri(baseUrl + "/standard/v1/payments/{reference}", externalReference)
                    .header("Authorization", "Basic " + basicToken(clientId, clientSecret))
                    .retrieve()
                    .body(Map.class);
            Object transaction = response == null ? null : response.get("transaction");
            String status = transaction instanceof Map<?, ?> map
                    ? String.valueOf(map.get("status"))
                    : "UNKNOWN";
            boolean success = "TS".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status);
            return new PaymentResult(success, externalReference, "Airtel status: " + status);
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("Airtel Money verification failed: " + ex.getResponseBodyAsString(), ex);
        }
    }

    private void ensureConfigured() {
        if (baseUrl == null || baseUrl.isBlank() || clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("Airtel Money integration is not configured");
        }
    }

    private String basicToken(String id, String secret) {
        return java.util.Base64.getEncoder()
                .encodeToString((id + ":" + secret).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
