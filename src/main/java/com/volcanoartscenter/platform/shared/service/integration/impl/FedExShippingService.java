package com.volcanoartscenter.platform.shared.service.integration.impl;

import com.volcanoartscenter.platform.shared.service.integration.ShippingCarrierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * FedEx integration: rate quote, ship label, and tracking. OAuth2 access tokens
 * (typ. ~3600s TTL) are cached in-process and refreshed 60s before expiry.
 */
@Service
@Slf4j
public class FedExShippingService implements ShippingCarrierService {

    private final RestClient restClient = RestClient.builder().build();
    private final String baseUrl;
    private final String apiKey;
    private final String apiSecret;
    private final String accountNumber;

    private volatile String cachedToken;
    private volatile Instant cachedTokenExpiresAt = Instant.EPOCH;

    public FedExShippingService(@Value("${platform.integrations.fedex.base-url:}") String baseUrl,
                                @Value("${platform.integrations.fedex.api-key:}") String apiKey,
                                @Value("${platform.integrations.fedex.api-secret:}") String apiSecret,
                                @Value("${platform.integrations.fedex.account-number:}") String accountNumber) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.accountNumber = accountNumber;
    }

    @Override
    public String provider() { return "FEDEX"; }

    public boolean isConfigured() {
        return !isBlank(baseUrl) && !isBlank(apiKey) && !isBlank(apiSecret) && !isBlank(accountNumber);
    }

    @Override
    public BigDecimal estimate(String destinationCountry, BigDecimal weightKg) {
        if ("Rwanda".equalsIgnoreCase(destinationCountry)) {
            return new BigDecimal("10.00");
        }
        ensureConfigured();
        try {
            String token = oauthToken();
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(baseUrl + "/rate/v1/rates/quotes")
                    .header("Authorization", "Bearer " + token)
                    .body(Map.of(
                            "accountNumber", Map.of("value", accountNumber),
                            "requestedShipment", Map.of(
                                    "shipper", Map.of("address", Map.of("countryCode", "RW")),
                                    "recipient", Map.of("address", Map.of("countryCode", toIso2(destinationCountry))),
                                    "pickupType", "DROPOFF_AT_FEDEX_LOCATION",
                                    "requestedPackageLineItems", List.of(Map.of(
                                            "weight", Map.of("units", "KG", "value", weightKg == null ? BigDecimal.ONE : weightKg)
                                    ))
                            )
                    ))
                    .retrieve()
                    .body(Map.class);
            return extractRate(response);
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("FedEx estimate failed: " + ex.getResponseBodyAsString(), ex);
        }
    }

    @Override
    public String createShipment(String reference) {
        ensureConfigured();
        try {
            String token = oauthToken();
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(baseUrl + "/ship/v1/shipments")
                    .header("Authorization", "Bearer " + token)
                    .body(Map.of(
                            "labelResponseOptions", "URL_ONLY",
                            "requestedShipment", Map.of(
                                    "shipDatestamp", LocalDate.now().toString(),
                                    "serviceType", "INTERNATIONAL_PRIORITY",
                                    "packagingType", "YOUR_PACKAGING",
                                    "shipper", Map.of("address", Map.of("countryCode", "RW")),
                                    "recipients", List.of(Map.of("address", Map.of("countryCode", "US"))),
                                    "pickupType", "DROPOFF_AT_FEDEX_LOCATION",
                                    "shippingChargesPayment", Map.of("paymentType", "SENDER"),
                                    "requestedPackageLineItems", List.of(Map.of(
                                            "weight", Map.of("units", "KG", "value", 1)
                                    )),
                                    "customerReferences", List.of(Map.of(
                                            "customerReferenceType", "CUSTOMER_REFERENCE",
                                            "value", reference))
                            ),
                            "accountNumber", Map.of("value", accountNumber)
                    ))
                    .retrieve()
                    .body(Map.class);
            return extractTrackingNumber(response);
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("FedEx shipment creation failed: " + ex.getResponseBodyAsString(), ex);
        }
    }

    @Override
    public Map<String, Object> track(String trackingNumber) {
        ensureConfigured();
        try {
            String token = oauthToken();
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(baseUrl + "/track/v1/trackingnumbers")
                    .header("Authorization", "Bearer " + token)
                    .header("X-locale", "en_US")
                    .body(Map.of(
                            "trackingInfo", List.of(Map.of(
                                    "trackingNumberInfo", Map.of("trackingNumber", trackingNumber)
                            )),
                            "includeDetailedScans", true
                    ))
                    .retrieve()
                    .body(Map.class);
            return summarizeTracking(trackingNumber, response);
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("FedEx tracking failed: " + ex.getResponseBodyAsString(), ex);
        }
    }

    private String oauthToken() {
        Instant now = Instant.now();
        if (cachedToken != null && now.isBefore(cachedTokenExpiresAt.minusSeconds(60))) {
            return cachedToken;
        }
        synchronized (this) {
            if (cachedToken != null && Instant.now().isBefore(cachedTokenExpiresAt.minusSeconds(60))) {
                return cachedToken;
            }
            String body = "grant_type=client_credentials"
                    + "&client_id=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(apiSecret, StandardCharsets.UTF_8);
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> response = restClient.post()
                        .uri(baseUrl + "/oauth/token")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .body(body)
                        .retrieve()
                        .body(Map.class);
                if (response == null || response.get("access_token") == null) {
                    throw new IllegalStateException("FedEx OAuth token missing");
                }
                this.cachedToken = String.valueOf(response.get("access_token"));
                long ttl = response.get("expires_in") instanceof Number n ? n.longValue() : 3600L;
                this.cachedTokenExpiresAt = Instant.now().plusSeconds(ttl);
                return cachedToken;
            } catch (RestClientResponseException ex) {
                throw new IllegalStateException("FedEx OAuth failed: " + ex.getResponseBodyAsString(), ex);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private BigDecimal extractRate(Map<String, Object> response) {
        if (response != null && response.get("output") instanceof Map<?, ?> output
                && output.get("rateReplyDetails") instanceof List<?> details && !details.isEmpty()
                && details.get(0) instanceof Map<?, ?> firstDetail
                && firstDetail.get("ratedShipmentDetails") instanceof List<?> rated && !rated.isEmpty()
                && rated.get(0) instanceof Map<?, ?> quote
                && quote.get("totalNetCharge") instanceof Map<?, ?> charge
                && charge.get("amount") != null) {
            return new BigDecimal(String.valueOf(charge.get("amount")));
        }
        throw new IllegalStateException("FedEx did not return a shipping quote");
    }

    @SuppressWarnings("unchecked")
    private String extractTrackingNumber(Map<String, Object> response) {
        if (response != null && response.get("output") instanceof Map<?, ?> output
                && output.get("transactionShipments") instanceof List<?> shipments && !shipments.isEmpty()
                && shipments.get(0) instanceof Map<?, ?> first) {
            Object master = first.get("masterTrackingNumber");
            if (master != null) return String.valueOf(master);
        }
        throw new IllegalStateException("FedEx did not return tracking number");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> summarizeTracking(String trackingNumber, Map<String, Object> response) {
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("trackingNumber", trackingNumber);
        out.put("supported", true);
        if (response != null && response.get("output") instanceof Map<?, ?> output
                && output.get("completeTrackResults") instanceof List<?> results && !results.isEmpty()
                && results.get(0) instanceof Map<?, ?> first
                && first.get("trackResults") instanceof List<?> tracks && !tracks.isEmpty()
                && tracks.get(0) instanceof Map<?, ?> firstTrack) {
            Map<String, Object> latest = (Map<String, Object>) firstTrack.get("latestStatusDetail");
            if (latest != null) {
                out.put("statusCode", latest.get("code"));
                out.put("statusDescription", latest.get("description"));
            }
            out.put("scanEvents", firstTrack.get("scanEvents"));
            out.put("estimatedDelivery", firstTrack.get("estimatedDeliveryTimeWindow"));
        }
        return out;
    }

    private String toIso2(String country) {
        if (country == null || country.isBlank()) return "US";
        return switch (country.trim().toLowerCase()) {
            case "rwanda" -> "RW";
            case "uganda" -> "UG";
            case "kenya" -> "KE";
            case "tanzania" -> "TZ";
            case "france" -> "FR";
            case "belgium" -> "BE";
            case "germany" -> "DE";
            case "united kingdom", "uk" -> "GB";
            case "united states", "usa", "us" -> "US";
            default -> "US";
        };
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException("FedEx integration is not configured");
        }
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String trimTrailingSlash(String s) {
        return (s != null && s.endsWith("/")) ? s.substring(0, s.length() - 1) : s;
    }
}
