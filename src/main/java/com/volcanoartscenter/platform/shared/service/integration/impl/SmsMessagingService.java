package com.volcanoartscenter.platform.shared.service.integration.impl;

import com.volcanoartscenter.platform.shared.service.integration.MessagingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Africa's Talking SMS adapter.
 *
 * <p>POST {@code https://api.africastalking.com/version1/messaging}
 * <br>Headers: {@code apiKey: <AT_API_KEY>}, {@code Accept: application/json}
 * <br>Body (form): {@code username, to, message}
 *
 * <p>Returns the messageId of the first recipient on success. Subject is
 * collapsed into the SMS body since SMS has no subject line.
 */
@Service
@Slf4j
public class SmsMessagingService implements MessagingService {

    private static final String DEFAULT_API_URL = "https://api.africastalking.com/version1/messaging";
    private static final int MAX_SMS_BODY_CHARS = 480;

    private final RestClient restClient;
    private final String apiUrl;
    private final String apiKey;
    private final String username;
    private final String senderId;
    private final boolean enabled;

    public SmsMessagingService(
            @Value("${platform.integrations.sms.africas-talking.api-url:" + DEFAULT_API_URL + "}") String apiUrl,
            @Value("${platform.integrations.sms.africas-talking.api-key:}") String apiKey,
            @Value("${platform.integrations.sms.africas-talking.username:}") String username,
            @Value("${platform.integrations.sms.africas-talking.sender-id:}") String senderId,
            @Value("${platform.notifications.sms.enabled:false}") boolean enabled) {
        this.apiUrl = (apiUrl == null || apiUrl.isBlank()) ? DEFAULT_API_URL : apiUrl;
        this.apiKey = apiKey;
        this.username = username;
        this.senderId = senderId;
        this.enabled = enabled;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public String channel() { return "SMS"; }

    @Override
    public boolean isConfigured() {
        return enabled
                && apiKey != null && !apiKey.isBlank()
                && username != null && !username.isBlank();
    }

    @Override
    public String send(String recipient, String subject, String body) {
        if (!isConfigured()) {
            throw new DeliveryException("Africa's Talking SMS is not configured");
        }
        String message = composeMessage(subject, body);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", username);
        form.add("to", recipient);
        form.add("message", message);
        if (senderId != null && !senderId.isBlank()) {
            form.add("from", senderId);
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("apiKey", apiKey)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            return extractMessageId(response);
        } catch (RestClientResponseException ex) {
            throw new DeliveryException("AT " + ex.getStatusCode() + ": " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            throw new DeliveryException("AT transport error: " + ex.getMessage(), ex);
        }
    }

    private String composeMessage(String subject, String body) {
        String text;
        if (subject == null || subject.isBlank()) {
            text = body == null ? "" : body;
        } else if (body == null || body.isBlank()) {
            text = subject;
        } else {
            text = subject + ": " + body;
        }
        return text.length() > MAX_SMS_BODY_CHARS ? text.substring(0, MAX_SMS_BODY_CHARS) : text;
    }

    @SuppressWarnings("unchecked")
    private String extractMessageId(Map<String, Object> response) {
        if (response == null) return "at-no-id";
        Object data = response.get("SMSMessageData");
        if (data instanceof Map<?, ?> dataMap) {
            Object recipients = ((Map<String, Object>) dataMap).get("Recipients");
            if (recipients instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
                Object id = ((Map<String, Object>) first).get("messageId");
                Object status = ((Map<String, Object>) first).get("status");
                if (status != null && !"Success".equalsIgnoreCase(String.valueOf(status))) {
                    throw new DeliveryException("AT recipient status: " + status);
                }
                if (id != null) return String.valueOf(id);
            }
        }
        return "at-no-id";
    }
}
