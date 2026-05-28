-- ===========================================================================
-- V005__notification_channels.sql
-- Phase 5: outbound channel hardening — adds retry-with-backoff bookkeeping
-- to notification_logs and admits the SMS channel for Africa's Talking.
-- ===========================================================================

ALTER TABLE public.notification_logs
    ADD COLUMN IF NOT EXISTS attempts        INT          NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS max_attempts    INT          NOT NULL DEFAULT 5,
    ADD COLUMN IF NOT EXISTS last_attempt_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS next_attempt_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS last_error      TEXT;

UPDATE public.notification_logs
   SET next_attempt_at = COALESCE(next_attempt_at, created_at, now())
 WHERE next_attempt_at IS NULL;

-- Replace any pre-existing channel check that excluded SMS.
DO $$
DECLARE
    cname text;
BEGIN
    SELECT conname INTO cname
      FROM pg_constraint
     WHERE conrelid = 'public.notification_logs'::regclass
       AND contype  = 'c'
       AND pg_get_constraintdef(oid) ILIKE '%channel%';
    IF cname IS NOT NULL THEN
        EXECUTE format('ALTER TABLE public.notification_logs DROP CONSTRAINT %I', cname);
    END IF;
END$$;

ALTER TABLE public.notification_logs
    ADD CONSTRAINT notification_logs_channel_check
        CHECK (channel IN ('EMAIL', 'WHATSAPP', 'SMS'));

CREATE INDEX IF NOT EXISTS idx_notif_logs_retry
    ON public.notification_logs (status, next_attempt_at);
