package com.volcanoartscenter.platform.shared.reference;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "reference_counters")
@IdClass(ReferenceCounter.Pk.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferenceCounter {

    @Id
    @Column(nullable = false, length = 40)
    private String scope;

    @Id
    @Column(nullable = false)
    private Integer year;

    @Column(name = "last_value", nullable = false)
    @Builder.Default
    private Long lastValue = 0L;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() { this.updatedAt = LocalDateTime.now(); }

    public static class Pk implements Serializable {
        private String scope;
        private Integer year;
        public Pk() {}
        public Pk(String scope, Integer year) { this.scope = scope; this.year = year; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk other)) return false;
            return Objects.equals(scope, other.scope) && Objects.equals(year, other.year);
        }
        @Override public int hashCode() { return Objects.hash(scope, year); }
    }
}
