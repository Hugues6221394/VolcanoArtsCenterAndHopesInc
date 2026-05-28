package com.volcanoartscenter.platform.shared.service.integration.impl;

import com.volcanoartscenter.platform.shared.service.integration.MessagingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Resend email adapter — https://resend.com/docs/api-reference/emails/send-email
 *
 * <p>POST {@code https://api.resend.com/emails}
 * <br>Headers: {@code Authorization: Bearer <RESEND_API_KEY>}
 * <br>Body: {@code {from, to[], subject, html}}
 *
 * <p>Returns the Resend message id as the external reference. Persistence + retry
 * are handled by {@code OutboundChannelService} — this adapter is pure transport.
 */
@Service
@Slf4j
public class EmailMessagingService implements MessagingService {

    private static final String DEFAULT_API_URL = "https://api.resend.com/emails";

    private final RestClient restClient;
    private final String apiUrl;
    private final String apiKey;
    private final String fromAddress;
    private final boolean enabled;

    public EmailMessagingService(
            @Value("${platform.integrations.resend.api-key:}") String apiKey,
            @Value("${platform.integrations.resend.mail-from:noreply@volcanoartscenter.rw}") String fromAddress,
            @Value("${platform.integrations.resend.api-url:" + DEFAULT_API_URL + "}") String apiUrl,
            @Value("${platform.notifications.email.enabled:true}") boolean enabled) {
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
        this.apiUrl = (apiUrl == null || apiUrl.isBlank()) ? DEFAULT_API_URL : apiUrl;
        this.enabled = enabled;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public String channel() { return "EMAIL"; }

    @Override
    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String send(String recipient, String subject, String body) {
        if (!isConfigured()) {
            throw new DeliveryException("Resend email is not configured");
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(Map.of(
                            "from", fromAddress,
                            "to", List.of(recipient),
                            "subject", subject == null ? "" : subject,
                            "html", body == null ? "" : body
                    ))
                    .retrieve()
                    .body(Map.class);
            String id = response == null ? null : String.valueOf(response.get("id"));
            return (id == null || "null".equals(id)) ? "resend-no-id" : id;
        } catch (RestClientResponseException ex) {
            throw new DeliveryException("Resend " + ex.getStatusCode() + ": " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            throw new DeliveryException("Resend transport error: " + ex.getMessage(), ex);
        }
    }
}
