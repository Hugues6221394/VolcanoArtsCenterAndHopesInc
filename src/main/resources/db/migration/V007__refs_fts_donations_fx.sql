-- ===========================================================================
-- V007__refs_fts_donations_fx.sql
-- Phase 8:
--   • VAC-YYYY-NNNNN reference counters (atomic per scope+year)
--   • Donation: ref + Stripe subscription / customer / impact tier
--   • Postgres FTS for products & experiences (tsvector + GIN + trigger)
--   • FX rate cache (Frankfurter daily rates)
-- ===========================================================================

-- ---------- Reference counters ----------
CREATE TABLE IF NOT EXISTS public.reference_counters (
    scope       VARCHAR(40)  NOT NULL,
    year        INT          NOT NULL,
    last_value  BIGINT       NOT NULL DEFAULT 0,
    updated_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (scope, year)
);

-- ---------- Donations ----------
ALTER TABLE public.donations
    ADD COLUMN IF NOT EXISTS reference            VARCHAR(40) UNIQUE,
    ADD COLUMN IF NOT EXISTS impact_tier_label    VARCHAR(200),
    ADD COLUMN IF NOT EXISTS stripe_customer_id   VARCHAR(120),
    ADD COLUMN IF NOT EXISTS stripe_subscription_id VARCHAR(120);

CREATE INDEX IF NOT EXISTS idx_donations_reference ON public.donations (reference);

-- ---------- Talent applications ----------
ALTER TABLE public.talent_applications
    ADD COLUMN IF NOT EXISTS reference VARCHAR(40) UNIQUE;

CREATE INDEX IF NOT EXISTS idx_talent_applications_reference ON public.talent_applications (reference);

-- ---------- FTS: products ----------
ALTER TABLE public.products
    ADD COLUMN IF NOT EXISTS search_vector tsvector;

UPDATE public.products
   SET search_vector = setweight(to_tsvector('simple', coalesce(name, '')), 'A')
                    || setweight(to_tsvector('simple', coalesce(short_description, '')), 'B')
                    || setweight(to_tsvector('simple', coalesce(artist_name, '')), 'B')
                    || setweight(to_tsvector('simple', coalesce(description, '')), 'C');

CREATE INDEX IF NOT EXISTS idx_products_search_vector ON public.products USING GIN (search_vector);

CREATE OR REPLACE FUNCTION public.products_search_vector_refresh() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
          setweight(to_tsvector('simple', coalesce(NEW.name, '')), 'A')
       || setweight(to_tsvector('simple', coalesce(NEW.short_description, '')), 'B')
       || setweight(to_tsvector('simple', coalesce(NEW.artist_name, '')), 'B')
       || setweight(to_tsvector('simple', coalesce(NEW.description, '')), 'C');
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_products_search_vector ON public.products;
CREATE TRIGGER trg_products_search_vector
    BEFORE INSERT OR UPDATE ON public.products
    FOR EACH ROW EXECUTE FUNCTION public.products_search_vector_refresh();

-- ---------- FTS: experiences ----------
ALTER TABLE public.experiences
    ADD COLUMN IF NOT EXISTS search_vector tsvector;

UPDATE public.experiences
   SET search_vector = setweight(to_tsvector('simple', coalesce(title, '')), 'A')
                    || setweight(to_tsvector('simple', coalesce(short_description, '')), 'B')
                    || setweight(to_tsvector('simple', coalesce(description, '')), 'C');

CREATE INDEX IF NOT EXISTS idx_experiences_search_vector ON public.experiences USING GIN (search_vector);

CREATE OR REPLACE FUNCTION public.experiences_search_vector_refresh() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
          setweight(to_tsvector('simple', coalesce(NEW.title, '')), 'A')
       || setweight(to_tsvector('simple', coalesce(NEW.short_description, '')), 'B')
       || setweight(to_tsvector('simple', coalesce(NEW.description, '')), 'C');
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_experiences_search_vector ON public.experiences;
CREATE TRIGGER trg_experiences_search_vector
    BEFORE INSERT OR UPDATE ON public.experiences
    FOR EACH ROW EXECUTE FUNCTION public.experiences_search_vector_refresh();

-- ---------- FX rate cache ----------
CREATE TABLE IF NOT EXISTS public.fx_rates (
    base_currency  CHAR(3)       NOT NULL,
    quote_currency CHAR(3)       NOT NULL,
    rate           NUMERIC(18,8) NOT NULL,
    fetched_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    source         VARCHAR(40)   NOT NULL DEFAULT 'frankfurter',
    PRIMARY KEY (base_currency, quote_currency)
);
