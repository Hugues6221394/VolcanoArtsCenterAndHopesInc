package com.volcanoartscenter.platform.shared.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class CaptchaService {

    private final RestClient restClient = RestClient.builder().build();

    @Value("${platform.captcha.enabled:false}")
    private boolean captchaEnabled;

    @Value("${platform.captcha.provider:hcaptcha}")
    private String provider;

    @Value("${platform.captcha.verify-url:}")
    private String verifyUrl;

    @Value("${platform.captcha.secret:}")
    private String secret;

    public boolean verify(String token) {
        if (!captchaEnabled) {
            return true;
        }
        if (token == null || token.isBlank()) {
            return false;
        }
        if (verifyUrl == null || verifyUrl.isBlank() || secret == null || secret.isBlank()) {
            throw new IllegalStateException("Captcha integration is enabled but not configured");
        }
        try {
            java.util.Map<?, ?> response = restClient.post()
                    .uri(verifyUrl)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body("secret=" + java.net.URLEncoder.encode(secret, java.nio.charset.StandardCharsets.UTF_8)
                            + "&response=" + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8))
                    .retrieve()
                    .body(java.util.Map.class);
            if (response == null) {
                return false;
            }
            Object successRaw = response.get("success");
            boolean success = successRaw instanceof Boolean b && b;
            if (!success) {
                return false;
            }
            if ("recaptcha".equalsIgnoreCase(provider) && response.get("score") instanceof Number score) {
                return score.doubleValue() >= 0.5;
            }
            return true;
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("Captcha verification failed: " + ex.getResponseBodyAsString(), ex);
        }
    }
}
