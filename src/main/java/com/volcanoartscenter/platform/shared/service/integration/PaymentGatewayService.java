package com.volcanoartscenter.platform.shared.service.integration;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentGatewayService {
    String provider();
    PaymentResult initialize(String reference, BigDecimal amount, String currency, Map<String, String> metadata);
    PaymentResult verify(String externalReference);

    record PaymentResult(boolean success, String externalReference, String message) {}
}
