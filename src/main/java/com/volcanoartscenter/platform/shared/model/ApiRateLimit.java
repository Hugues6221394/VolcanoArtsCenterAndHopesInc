package com.volcanoartscenter.platform.shared.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "api_rate_limits")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ApiRateLimit {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id; // Typically the IP address or client ID

    @Column(columnDefinition = "BYTEA")
    private byte[] state;
}
