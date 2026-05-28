package com.volcanoartscenter.platform.shared.payment;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_status_history", indexes = {
        @Index(name = "idx_osh_order_created", columnList = "order_id, created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistory {

    public enum Actor { USER, STAFF, SYSTEM, WEBHOOK, SIMULATED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "previous_status", length = 20)
    private String previousStatus;

    @Column(name = "new_status", length = 20)
    private String newStatus;

    @Column(name = "previous_payment_status", length = 20)
    private String previousPaymentStatus;

    @Column(name = "new_payment_status", length = 20)
    private String newPaymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Actor actor;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
