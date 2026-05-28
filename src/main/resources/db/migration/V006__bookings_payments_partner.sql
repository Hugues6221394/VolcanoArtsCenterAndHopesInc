-- ===========================================================================
-- V006__bookings_payments_partner.sql
-- Phase 6: deposit + cancellation bookkeeping for bookings.
-- Stripe / MTN MoMo / Bank Transfer all flow through the polymorphic Payment
-- ledger introduced in V004 — no schema changes required there.
-- ===========================================================================

ALTER TABLE public.bookings
    ADD COLUMN IF NOT EXISTS deposit_amount      NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS deposit_required    BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS payment_due_at      TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS cancelled_at        TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS payment_reference   VARCHAR(120);

CREATE INDEX IF NOT EXISTS idx_bookings_payment_reference
    ON public.bookings (payment_reference);
