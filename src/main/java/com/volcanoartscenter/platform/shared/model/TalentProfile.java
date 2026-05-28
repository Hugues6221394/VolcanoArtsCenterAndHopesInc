package com.volcanoartscenter.platform.shared.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "talent_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TalentProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private TalentApplication.ApplicantCategory category;

    @Column(name = "headline", length = 250)
    private String headline;

    @Column(name = "story", columnDefinition = "TEXT")
    private String story;

    @Column(name = "primary_image_url", length = 700)
    private String primaryImageUrl;

    @Column(name = "primary_media_id")
    private Long primaryMediaId;

    @Column(name = "published", nullable = false)
    @Builder.Default
    private Boolean published = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
