-- ===========================================================================
-- V008__fx_rates_varchar.sql
-- V007 created fx_rates.base_currency and quote_currency as CHAR(3) (Postgres
-- bpchar). The JPA entity maps them as String with @Column(length = 3), which
-- Hibernate validates as VARCHAR(3). Schema validation fails on boot.
--
-- This migration converts both columns to VARCHAR(3) so Hibernate validates
-- cleanly. ALTER TYPE is fast on a typically-small fx_rates table; no data
-- shape change (the trailing space-padding behaviour of CHAR is dropped).
-- ===========================================================================

ALTER TABLE public.fx_rates
    ALTER COLUMN base_currency  TYPE VARCHAR(3) USING TRIM(base_currency),
    ALTER COLUMN quote_currency TYPE VARCHAR(3) USING TRIM(quote_currency);
