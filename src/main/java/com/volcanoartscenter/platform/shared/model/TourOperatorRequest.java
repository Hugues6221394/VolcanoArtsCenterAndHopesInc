package com.volcanoartscenter.platform.shared.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tour_operator_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourOperatorRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "contact_name", nullable = false, length = 150)
    private String contactName;

    @Column(name = "contact_email", nullable = false, length = 150)
    private String contactEmail;

    @Column(name = "owner_email", length = 150)
    private String ownerEmail;

    @Column(name = "contact_phone", length = 40)
    private String contactPhone;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "requested_experience_slug", length = 250)
    private String requestedExperienceSlug;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", length = 30)
    @Builder.Default
    private RequestType requestType = RequestType.GROUP_BOOKING;

    @Column(name = "estimated_group_size")
    private Integer estimatedGroupSize;

    @Column(name = "estimated_date")
    private LocalDate estimatedDate;

    @Column(name = "invoice_required", nullable = false)
    @Builder.Default
    private Boolean invoiceRequired = true;

    @Column(name = "request_details", columnDefinition = "TEXT")
    private String requestDetails;

    @Column(name = "partner_price", precision = 10, scale = 2)
    private java.math.BigDecimal partnerPrice;

    @Column(name = "partner_price_currency", length = 3)
    private String partnerPriceCurrency;

    @Column(name = "itinerary_asset_url", length = 700)
    private String itineraryAssetUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private RequestStatus status = RequestStatus.SUBMITTED;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "preferred_contact_channel", length = 20)
    private String preferredContactChannel;

    @Column(name = "last_notified_channel", length = 20)
    private String lastNotifiedChannel;

    @Column(name = "last_notified_at")
    private LocalDateTime lastNotifiedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum RequestStatus {
        SUBMITTED,
        UNDER_REVIEW,
        INVOICE_PENDING,
        CONFIRMED,
        DECLINED
    }

    public enum RequestType {
        GROUP_BOOKING,
        CUSTOM_PACKAGE
    }
}
