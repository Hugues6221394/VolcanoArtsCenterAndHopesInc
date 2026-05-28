-- ===========================================================================
-- V004__payments_and_order_history.sql
-- Phase 4: polymorphic Payment ledger + ShippingOrder status history.
-- Payment rows live for orders, bookings, donations, talent fees — anything
-- that can be paid for. gateway+gateway_ref unique avoids duplicate processing
-- when a webhook is replayed.
-- ===========================================================================

CREATE TABLE public.payments (
    id              BIGSERIAL    PRIMARY KEY,
    gateway         VARCHAR(20)  NOT NULL,
    gateway_ref     VARCHAR(255) NOT NULL,
    source_type     VARCHAR(20)  NOT NULL,
    source_id       BIGINT       NOT NULL,
    amount          NUMERIC(12,2) NOT NULL,
    currency        VARCHAR(3)   NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    payer_user_id   BIGINT       REFERENCES public.users(id) ON DELETE SET NULL,
    payer_email     VARCHAR(160),
    failure_reason  TEXT,
    captured_at     TIMESTAMP WITHOUT TIME ZONE,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT payments_gateway_check
        CHECK (gateway IN ('STRIPE_CARD','MTN_MOMO','BANK_TRANSFER','MANUAL')),
    CONSTRAINT payments_source_type_check
        CHECK (source_type IN ('SHIPPING_ORDER','BOOKING','DONATION','TALENT_FEE')),
    CONSTRAINT payments_status_check
        CHECK (status IN ('PENDING','AUTHORIZED','CAPTURED','FAILED','REFUNDED','CANCELLED')),
    CONSTRAINT uq_payments_gateway_ref UNIQUE (gateway, gateway_ref)
);

CREATE INDEX idx_payments_source       ON public.payments (source_type, source_id);
CREATE INDEX idx_payments_payer_user   ON public.payments (payer_user_id, created_at DESC);
CREATE INDEX idx_payments_status       ON public.payments (status);

CREATE TABLE public.order_status_history (
    id                       BIGSERIAL    PRIMARY KEY,
    order_id                 BIGINT       NOT NULL REFERENCES public.shipping_orders(id) ON DELETE CASCADE,
    previous_status          VARCHAR(20),
    new_status               VARCHAR(20),
    previous_payment_status  VARCHAR(20),
    new_payment_status       VARCHAR(20),
    actor                    VARCHAR(20)  NOT NULL,
    actor_user_id            BIGINT       REFERENCES public.users(id) ON DELETE SET NULL,
    reason                   VARCHAR(500),
    created_at               TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT osh_actor_check
        CHECK (actor IN ('USER','STAFF','SYSTEM','WEBHOOK','SIMULATED'))
);

CREATE INDEX idx_osh_order_created ON public.order_status_history (order_id, created_at DESC);
