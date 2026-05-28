package com.volcanoartscenter.platform.shared.notification;

/**
 * Enumerates the events that produce a user-facing notification (PRD §8 trigger table).
 * Outbound channel adapters (email/WhatsApp/SMS) are wired in Phase 5; Phase 2 covers
 * the in-app inbox only.
 */
public enum NotificationCategory {
    ACCOUNT_CREATED,

    ORDER_RECEIVED,
    ORDER_CONFIRMED,
    ORDER_SHIPPED,
    ORDER_DELIVERED,

    BOOKING_RECEIVED,
    BOOKING_CONFIRMED,
    BOOKING_REJECTED,

    PAYMENT_FAILED,

    DONATION_RECEIVED,

    TALENT_APPLICATION_RECEIVED,
    TALENT_STATUS_CHANGED,

    PASSWORD_RESET,

    MESSAGE_RECEIVED,

    /** Internal: shows up in staff dashboards. */
    STAFF_NEW_ORDER,
    STAFF_NEW_BOOKING,
    STAFF_NEW_TALENT_APPLICATION,
    STAFF_NEW_INQUIRY,
    STAFF_NEW_OPERATOR_REQUEST
}
