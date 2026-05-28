package com.volcanoartscenter.platform.shared.notification;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.ZoneOffset;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationDto(
        Long id,
        String category,
        String title,
        String body,
        String ctaUrl,
        String entityType,
        Long entityId,
        Instant readAt,
        Instant createdAt,
        boolean unread
) {
    public static NotificationDto from(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getCategory() == null ? null : n.getCategory().name(),
                n.getTitle(),
                n.getBody(),
                n.getCtaUrl(),
                n.getEntityType(),
                n.getEntityId(),
                n.getReadAt() == null ? null : n.getReadAt().toInstant(ZoneOffset.UTC),
                n.getCreatedAt() == null ? null : n.getCreatedAt().toInstant(ZoneOffset.UTC),
                n.isUnread()
        );
    }
}
