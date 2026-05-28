package com.volcanoartscenter.platform.shared.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "talent_applications")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TalentApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "age_range", length = 30)
    private String ageRange;

    @Column(length = 20)
    private String gender;

    @Column(length = 200)
    private String location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Human-readable reference number (e.g. "VAC-2026-00142")
    @Column(name = "reference_number", unique = true, length = 30)
    private String referenceNumber;

    // Category of applicant
    @Enumerated(EnumType.STRING)
    @Column(name = "applicant_category", nullable = false, length = 30)
    private ApplicantCategory applicantCategory;

    // Area of interest
    @Enumerated(EnumType.STRING)
    @Column(name = "talent_area", nullable = false, length = 30)
    private TalentArea talentArea;

    @Column(name = "experience_description", columnDefinition = "TEXT")
    private String experienceDescription;

    @Column(columnDefinition = "TEXT")
    private String motivation;

    // Availability
    @Column(name = "availability_details", length = 500)
    private String availabilityDetails;

    // Disability support needs
    @Column(name = "accessibility_needs", length = 500)
    private String accessibilityNeeds;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    // Feedback shown to the applicant on rejection or info request
    @Column(name = "feedback_to_applicant", columnDefinition = "TEXT")
    private String feedbackToApplicant;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "preferred_contact_channel", length = 20)
    private String preferredContactChannel;

    @Column(name = "last_notified_channel", length = 20)
    private String lastNotifiedChannel;

    @Column(name = "last_notified_at")
    private LocalDateTime lastNotifiedAt;

    // Timestamps
    @Column(unique = true, length = 40)
    private String reference;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        // Auto-generate reference number
        if (referenceNumber == null) {
            referenceNumber = "VAC-" + java.time.Year.now().getValue() + "-"
                    + String.format("%05d", System.nanoTime() % 100000);
        }
    }


    public enum ApplicantCategory {
        YOUTH,
        SINGLE_MOTHER,
        PERSON_WITH_DISABILITY,
        ELDERLY,
        PROFESSIONAL_ARTIST,
        COMMUNITY_MEMBER
    }

    public enum TalentArea {
        TRADITIONAL_DANCE,
        STORYTELLING,
        CULTURAL_PERFORMANCE,
        MUSIC,
        VISUAL_ARTS,
        CRAFTS,
        OTHER
    }

    public enum ApplicationStatus {
        PENDING,          // Submitted, awaiting review
        APPROVED,         // Accepted into the talent program
        REJECTED,         // Declined with feedback
        AWAITING_INFO,    // Staff requested additional information
        ARCHIVED          // Closed / no further action
    }
}
