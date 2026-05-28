package com.volcanoartscenter.platform.shared.messaging;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversations", indexes = {
        @Index(name = "idx_conversations_opened_by", columnList = "opened_by_user_id, last_message_at DESC"),
        @Index(name = "idx_conversations_assigned_staff", columnList = "assigned_staff_user_id, last_message_at DESC"),
        @Index(name = "idx_conversations_status", columnList = "status, last_message_at DESC"),
        @Index(name = "idx_conversations_product", columnList = "product_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversationStatus status;

    @Column(name = "opened_by_user_id", nullable = false)
    private Long openedByUserId;

    @Column(name = "assigned_staff_user_id")
    private Long assignedStaffUserId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "last_message_at", nullable = false)
    private LocalDateTime lastMessageAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_message_by_role", nullable = false, length = 20)
    private MessageSenderRole lastMessageByRole;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (lastMessageAt == null) lastMessageAt = now;
        if (status == null) status = ConversationStatus.OPEN;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
