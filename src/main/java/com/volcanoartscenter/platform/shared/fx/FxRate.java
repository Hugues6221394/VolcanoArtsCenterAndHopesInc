package com.volcanoartscenter.platform.shared.fx;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "fx_rates")
@IdClass(FxRate.Pk.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FxRate {

    @Id
    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    @Id
    @Column(name = "quote_currency", nullable = false, length = 3)
    private String quoteCurrency;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal rate;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String source = "frankfurter";

    @PrePersist
    @PreUpdate
    void touch() {
        if (fetchedAt == null) fetchedAt = LocalDateTime.now();
    }

    public static class Pk implements Serializable {
        private String baseCurrency;
        private String quoteCurrency;
        public Pk() {}
        public Pk(String baseCurrency, String quoteCurrency) {
            this.baseCurrency = baseCurrency;
            this.quoteCurrency = quoteCurrency;
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk other)) return false;
            return Objects.equals(baseCurrency, other.baseCurrency)
                    && Objects.equals(quoteCurrency, other.quoteCurrency);
        }
        @Override public int hashCode() { return Objects.hash(baseCurrency, quoteCurrency); }
    }
}
