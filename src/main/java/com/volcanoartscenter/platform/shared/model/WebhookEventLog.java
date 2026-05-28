package com.volcanoartscenter.platform.shared.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_event_logs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class WebhookEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The unique event ID provided by Stripe (e.g. evt_1Oxyz...)
    @Column(name = "external_event_id", unique = true, nullable = false, length = 100)
    private String externalEventId;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider; // e.g. "STRIPE"

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    // PROCESSED, FAILED, IGNORED
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "processed_at", updatable = false)
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        processedAt = LocalDateTime.now();
    }
}
