package com.volcanoartscenter.platform.shared.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "consent_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject_email", nullable = false, length = 150)
    private String subjectEmail;

    @Column(name = "consent_type", nullable = false, length = 80)
    private String consentType;

    @Column(name = "consented", nullable = false)
    private Boolean consented;

    @Column(name = "source", length = 120)
    private String source;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
