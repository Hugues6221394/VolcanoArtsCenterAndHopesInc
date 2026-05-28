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
 * WhatsApp Cloud API adapter — Meta Graph API.
 *
 * <p>POST {@code https://graph.facebook.com/v22.0/{phone-number-id}/messages}
 * <br>Headers: {@code Authorization: Bearer <WHATSAPP_API_TOKEN>}
 * <br>Body: {@code {messaging_product:"whatsapp", to, type:"text", text:{body}}}
 *
 * <p>Returns the first message id from {@code messages[]} as the external
 * reference. Persistence + retry are owned by {@code OutboundChannelService}.
 */
@Service
@Slf4j
public class WhatsAppMessagingService implements MessagingService {

    private static final String GRAPH_BASE = "https://graph.facebook.com/v22.0";

    private final RestClient restClient;
    private final String apiBase;
    private final String apiToken;
    private final String phoneNumberId;
    private final boolean enabled;

    public WhatsAppMessagingService(
            @Value("${platform.integrations.whatsapp.api-url:" + GRAPH_BASE + "}") String apiBase,
            @Value("${platform.integrations.whatsapp.api-token:}") String apiToken,
            @Value("${platform.integrations.whatsapp.phone-number-id:}") String phoneNumberId,
            @Value("${platform.notifications.whatsapp.enabled:false}") boolean enabled) {
        this.apiBase = (apiBase == null || apiBase.isBlank()) ? GRAPH_BASE : apiBase;
        this.apiToken = apiToken;
        this.phoneNumberId = phoneNumberId;
        this.enabled = enabled;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public String channel() { return "WHATSAPP"; }

    @Override
    public boolean isConfigured() {
        return enabled
                && apiToken != null && !apiToken.isBlank()
                && phoneNumberId != null && !phoneNumberId.isBlank();
    }

    @Override
    public String send(String recipient, String subject, String body) {
        if (!isConfigured()) {
            throw new DeliveryException("WhatsApp Cloud API is not configured");
        }
        String text = (subject == null || subject.isBlank())
                ? (body == null ? "" : body)
                : (body == null || body.isBlank() ? subject : subject + "\n\n" + body);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(apiBase + "/" + phoneNumberId + "/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiToken)
                    .body(Map.of(
                            "messaging_product", "whatsapp",
                            "to", recipient,
                            "type", "text",
                            "text", Map.of("body", text)
                    ))
                    .retrieve()
                    .body(Map.class);
            return extractMessageId(response);
        } catch (RestClientResponseException ex) {
            throw new DeliveryException("WhatsApp " + ex.getStatusCode() + ": " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            throw new DeliveryException("WhatsApp transport error: " + ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractMessageId(Map<String, Object> response) {
        if (response == null) return "whatsapp-no-id";
        Object messages = response.get("messages");
        if (messages instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            Object id = ((Map<String, Object>) first).get("id");
            if (id != null) return String.valueOf(id);
        }
        return "whatsapp-no-id";
    }
}
