package com.volcanoartscenter.platform.shared.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "donation_campaigns")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DonationCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Dynamic impact statement: "$25 plants 5 trees in the Volcanoes buffer zone"
    @Column(name = "impact_statement", length = 500)
    private String impactStatement;

    // JSON map of { amount: description } tiers for dynamic frontend rendering
    // e.g. {"10":"funds one session","25":"plants 5 trees","50":"sponsors one community educator"}
    @Column(name = "impact_tiers", columnDefinition = "TEXT")
    private String impactTiers;

    @Column(name = "goal_amount", precision = 12, scale = 2)
    private BigDecimal goalAmount;

    @Column(name = "raised_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal raisedAmount = BigDecimal.ZERO;

    @Column(name = "donor_count")
    @Builder.Default
    private Integer donorCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "image_url")
    private String imageUrl;

    // Timestamps
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
}
