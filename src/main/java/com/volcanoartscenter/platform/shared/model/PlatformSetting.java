package com.volcanoartscenter.platform.shared.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "platform_settings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_platform_setting_category_key", columnNames = {"category", "key_name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(name = "key_name", nullable = false, length = 120)
    private String keyName;

    @Column(name = "value_data", columnDefinition = "TEXT")
    private String valueData;

    @Column(length = 300)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean masked = false;

    @Column(name = "updated_by", length = 150)
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAt = LocalDateTime.now();
    }
}
