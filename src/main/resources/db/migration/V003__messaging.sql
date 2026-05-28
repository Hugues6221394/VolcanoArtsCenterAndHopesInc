-- ===========================================================================
-- V003__messaging.sql
-- Phase 3: client <-> staff messaging.
-- A conversation can be product-scoped ("Ask about this artwork") or general.
-- ===========================================================================

CREATE TABLE public.conversations (
    id                       BIGSERIAL    PRIMARY KEY,
    subject                  VARCHAR(200) NOT NULL,
    status                   VARCHAR(20)  NOT NULL,
    opened_by_user_id        BIGINT       NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    assigned_staff_user_id   BIGINT       REFERENCES public.users(id) ON DELETE SET NULL,
    product_id               BIGINT       REFERENCES public.products(id) ON DELETE SET NULL,
    last_message_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_message_by_role     VARCHAR(20)  NOT NULL,
    created_at               TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at               TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT conversations_status_check
        CHECK (status IN ('OPEN','AWAITING_STAFF','AWAITING_CLIENT','RESOLVED')),
    CONSTRAINT conversations_last_msg_role_check
        CHECK (last_message_by_role IN ('CLIENT','STAFF'))
);

CREATE INDEX idx_conversations_opened_by
    ON public.conversations (opened_by_user_id, last_message_at DESC);

CREATE INDEX idx_conversations_assigned_staff
    ON public.conversations (assigned_staff_user_id, last_message_at DESC);

CREATE INDEX idx_conversations_status
    ON public.conversations (status, last_message_at DESC);

CREATE INDEX idx_conversations_product
    ON public.conversations (product_id);

CREATE TABLE public.messages (
    id                       BIGSERIAL    PRIMARY KEY,
    conversation_id          BIGINT       NOT NULL REFERENCES public.conversations(id) ON DELETE CASCADE,
    sender_user_id           BIGINT       NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    sender_role              VARCHAR(20)  NOT NULL,
    body                     TEXT         NOT NULL,
    read_by_recipient_at     TIMESTAMP WITHOUT TIME ZONE,
    created_at               TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT messages_sender_role_check
        CHECK (sender_role IN ('CLIENT','STAFF'))
);

CREATE INDEX idx_messages_conversation_created
    ON public.messages (conversation_id, created_at);

CREATE INDEX idx_messages_unread_for_recipient
    ON public.messages (conversation_id, sender_role, read_by_recipient_at);
