package com.volcanoartscenter.platform.shared.service.integration;

/**
 * Outbound message channel adapter (Email / WhatsApp / SMS).
 *
 * <p>Adapters are pure transport: they call the provider, return the provider's
 * external reference on success, or throw {@link DeliveryException} on a transport
 * or business failure. They MUST NOT write to {@code notification_logs} themselves —
 * persistence and retry bookkeeping live in {@code OutboundChannelService}.
 */
public interface MessagingService {

    String channel();

    /**
     * Send the message synchronously. Returns the provider's external reference
     * (e.g. Resend message id, WhatsApp message id, AT message id) on success.
     * Throws {@link DeliveryException} on any transport or non-2xx response so
     * the caller can record the failure and schedule a retry.
     */
    String send(String recipient, String subject, String body);

    boolean isConfigured();

    /** Marker exception for delivery failures eligible for retry-with-backoff. */
    class DeliveryException extends RuntimeException {
        public DeliveryException(String message) { super(message); }
        public DeliveryException(String message, Throwable cause) { super(message, cause); }
    }
}
