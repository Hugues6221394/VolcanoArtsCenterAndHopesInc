package com.volcanoartscenter.platform.shared.notification;

/**
 * Application event published from domain services on state transitions.
 * Picked up by {@link NotificationEventListener} after the originating transaction
 * commits — guarantees no "ghost" notifications when the originating write rolls back.
 */
public record NotificationEvent(
        Long recipientUserId,
        NotificationCategory category,
        String title,
        String body,
        String ctaUrl,
        String entityType,
        Long entityId
) {
    public static NotificationEvent of(Long userId, NotificationCategory category, String title, String body) {
        return new NotificationEvent(userId, category, title, body, null, null, null);
    }

    public static NotificationEvent of(Long userId, NotificationCategory category, String title, String body, String ctaUrl) {
        return new NotificationEvent(userId, category, title, body, ctaUrl, null, null);
    }

    public static NotificationEvent forEntity(Long userId, NotificationCategory category, String title, String body,
                                              String ctaUrl, String entityType, Long entityId) {
        return new NotificationEvent(userId, category, title, body, ctaUrl, entityType, entityId);
    }
}
