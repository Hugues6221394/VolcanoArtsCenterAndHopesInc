package com.volcanoartscenter.platform.shared.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reviewer_name", nullable = false, length = 150)
    private String reviewerName;

    @Column(name = "reviewer_email", length = 150)
    private String reviewerEmail;

    @Column(name = "reviewer_country", length = 100)
    private String reviewerCountry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Integer rating; // 1-5

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;

    // Can review a product or an experience
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experience_id")
    private Experience experience;

    // Moderation
    @Column(nullable = false)
    @Builder.Default
    private Boolean approved = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean featured = false;

    // Timestamps
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
