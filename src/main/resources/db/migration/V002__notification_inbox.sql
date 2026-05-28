-- ===========================================================================
-- V002__notification_inbox.sql
-- Phase 2: in-app notification inbox (PRD §8 trigger table — in-app channel).
-- The legacy notification_logs table from V001 stays as the channel-delivery
-- audit log; this new `notifications` table is the user-facing inbox.
-- ===========================================================================

CREATE TABLE public.notifications (
    id                BIGSERIAL    PRIMARY KEY,
    recipient_user_id BIGINT       NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    category          VARCHAR(40)  NOT NULL,
    title             VARCHAR(200) NOT NULL,
    body              TEXT,
    cta_url           VARCHAR(500),
    entity_type       VARCHAR(40),
    entity_id         BIGINT,
    read_at           TIMESTAMP WITHOUT TIME ZONE,
    created_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_recipient_created
    ON public.notifications (recipient_user_id, created_at DESC);

CREATE INDEX idx_notifications_recipient_unread
    ON public.notifications (recipient_user_id, read_at);

CREATE INDEX idx_notifications_category
    ON public.notifications (category);
