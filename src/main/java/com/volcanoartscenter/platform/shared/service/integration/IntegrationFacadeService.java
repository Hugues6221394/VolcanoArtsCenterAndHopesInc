package com.volcanoartscenter.platform.shared.service.integration;

import com.volcanoartscenter.platform.shared.model.AuditEvent;
import com.volcanoartscenter.platform.shared.model.NotificationLog;
import com.volcanoartscenter.platform.shared.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IntegrationFacadeService {

    private final List<PaymentGatewayService> paymentGatewayServices;
    private final List<ShippingCarrierService> shippingCarrierServices;
    private final List<MessagingService> messagingServices;
    private final OutboundChannelService outboundChannelService;
    private final AuditEventRepository auditEventRepository;
    private java.util.Map<String, PaymentGatewayService> paymentGatewayIndex;
    private java.util.Map<String, ShippingCarrierService> shippingCarrierIndex;
    private java.util.Map<String, MessagingService> messagingIndex;

    @jakarta.annotation.PostConstruct
    void initIndexes() {
        paymentGatewayIndex = paymentGatewayServices.stream()
                .collect(Collectors.toMap(s -> s.provider().toUpperCase(), Function.identity()));
        shippingCarrierIndex = shippingCarrierServices.stream()
                .collect(Collectors.toMap(s -> s.provider().toUpperCase(), Function.identity()));
        messagingIndex = messagingServices.stream()
                .collect(Collectors.toMap(s -> s.channel().toUpperCase(), Function.identity()));
    }

    public PaymentGatewayService.PaymentResult initializePayment(String provider, String reference, BigDecimal amount, String currency, Map<String, String> metadata) {
        PaymentGatewayService gateway = paymentGatewayIndex.get(normalize(provider));
        if (gateway == null) {
            throw new IllegalArgumentException("Unsupported payment provider: " + provider);
        }
        PaymentGatewayService.PaymentResult result = gateway.initialize(reference, amount, currency, metadata);
        auditEventRepository.save(AuditEvent.builder()
                .eventType("PAYMENT_INIT")
                .entityType("Payment")
                .details(provider + ":" + result.message())
                .build());
        return result;
    }

    public BigDecimal estimateShipping(String provider, String country, BigDecimal weightKg) {
        ShippingCarrierService carrier = shippingCarrierIndex.get(normalize(provider));
        if (carrier == null) {
            throw new IllegalArgumentException("Unsupported shipping provider: " + provider);
        }
        BigDecimal estimate = carrier.estimate(country, weightKg);
        auditEventRepository.save(AuditEvent.builder()
                .eventType("SHIPPING_ESTIMATE")
                .entityType("Shipping")
                .details(provider + ":" + country + ":" + estimate)
                .build());
        return estimate;
    }

    public String createShipment(String provider, String reference) {
        ShippingCarrierService carrier = shippingCarrierIndex.get(normalize(provider));
        if (carrier == null) {
            throw new IllegalArgumentException("Unsupported shipping provider: " + provider);
        }
        String tracking = carrier.createShipment(reference);
        auditEventRepository.save(AuditEvent.builder()
                .eventType("SHIPMENT_CREATED")
                .entityType("Shipping")
                .details(provider + ":" + tracking)
                .build());
        return tracking;
    }

    public PaymentGatewayService.PaymentResult verifyPayment(String provider, String externalReference) {
        PaymentGatewayService gateway = paymentGatewayIndex.get(normalize(provider));
        if (gateway == null) {
            throw new IllegalArgumentException("Unsupported payment provider: " + provider);
        }
        PaymentGatewayService.PaymentResult result = gateway.verify(externalReference);
        auditEventRepository.save(AuditEvent.builder()
                .eventType("PAYMENT_VERIFY")
                .entityType("Payment")
                .details(provider + ":" + result.message())
                .build());
        return result;
    }

    public void sendEmail(String recipient, String subject, String body) {
        outboundChannelService.deliver(NotificationLog.Channel.EMAIL, recipient, subject, body);
        auditEventRepository.save(AuditEvent.builder()
                .eventType("NOTIFICATION_SENT")
                .entityType("Message")
                .details("EMAIL:" + recipient + ":" + subject)
                .build());
    }

    public void sendWhatsApp(String recipient, String subject, String body) {
        outboundChannelService.deliver(NotificationLog.Channel.WHATSAPP, recipient, subject, body);
        auditEventRepository.save(AuditEvent.builder()
                .eventType("NOTIFICATION_SENT")
                .entityType("Message")
                .details("WHATSAPP:" + recipient + ":" + subject)
                .build());
    }

    public void sendSms(String recipient, String subject, String body) {
        outboundChannelService.deliver(NotificationLog.Channel.SMS, recipient, subject, body);
        auditEventRepository.save(AuditEvent.builder()
                .eventType("NOTIFICATION_SENT")
                .entityType("Message")
                .details("SMS:" + recipient + ":" + subject)
                .build());
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase();
    }
}
