# Volcano Arts Center — Production Deployment

End-to-end runbook for shipping this backend to Railway with a custom domain
and live third-party integrations.

---

## 1. Prerequisites

- Railway account + a project for `volcano-platform`.
- Domain name (e.g. `volcanoartscenter.rw`) with DNS access.
- Live API credentials for: Stripe, MTN MoMo, Resend, Africa's Talking, Meta
  WhatsApp Cloud, FedEx, AWS S3.
- A Railway-managed Postgres (or Neon/Supabase) and Redis instance.

---

## 2. Architecture summary

| Component            | Where it runs                              |
|----------------------|--------------------------------------------|
| Spring Boot service  | Railway (Docker, see `Dockerfile`)         |
| Postgres             | Railway add-on or Neon/Supabase            |
| Redis                | Railway add-on (sessions, rate limit, idem)|
| S3                   | AWS S3 (uploads + image variants)          |
| Email (Resend)       | api.resend.com                             |
| WhatsApp Cloud       | graph.facebook.com                         |
| SMS                  | api.africastalking.com                     |
| Card payments        | Stripe                                     |
| Mobile money         | MTN MoMo Collections                       |
| Bank transfer        | manual confirm via `/api/v1/ops/payments`  |
| Shipping             | FedEx (international) + manual local       |

---

## 3. Required environment variables

Set these in Railway → Service → Variables. **Never commit them to git.**

### 3.1 Core

| Var                         | Example                                        |
|-----------------------------|------------------------------------------------|
| `SPRING_PROFILES_ACTIVE`    | `prod`                                         |
| `PORT`                      | (Railway sets automatically)                   |
| `SPRING_DATASOURCE_URL`     | `jdbc:postgresql://host:5432/db`               |
| `SPRING_DATASOURCE_USERNAME`| `postgres`                                     |
| `SPRING_DATASOURCE_PASSWORD`| (secret)                                       |
| `REDIS_URL`                 | `redis://default:pass@host:6379`               |

### 3.2 Stripe

| Var                          | Notes                                                  |
|------------------------------|--------------------------------------------------------|
| `STRIPE_SECRET_KEY`          | `sk_live_...`                                          |
| `STRIPE_PUBLISHABLE_KEY`     | `pk_live_...`                                          |
| `STRIPE_WEBHOOK_SECRET`      | `whsec_...` from the Stripe dashboard webhook for `/api/v1/webhooks/stripe` |

### 3.3 MTN MoMo

| Var                         | Notes                                                   |
|-----------------------------|---------------------------------------------------------|
| `MTN_MOMO_BASE_URL`         | `https://momodeveloper.mtn.com` (or production URL)     |
| `MTN_MOMO_API_USER_ID`      | UUID created via MoMo developer portal                  |
| `MTN_MOMO_API_KEY`          | Secret returned by `/v1_0/apiuser/{uuid}/apikey`        |
| `MTN_MOMO_SUBSCRIPTION_KEY` | "Primary key" from your subscription                    |
| `MTN_MOMO_TARGET_ENV`       | `sandbox` or `mtnrwanda`                                |
| `MTN_MOMO_CURRENCY`         | `EUR` for sandbox, `RWF` for prod                       |
| `MTN_MOMO_CALLBACK_URL`     | `https://api.volcanoartscenter.rw/api/v1/webhooks/momo` |

### 3.4 Bank transfer (display-only)

| Var                  | Notes                            |
|----------------------|----------------------------------|
| `BANK_ACCOUNT_NAME`  | `Volcano Arts Center Inc.`       |
| `BANK_ACCOUNT_NUMBER`| Bank account number              |
| `BANK_NAME`          | e.g. `Bank of Kigali`            |
| `BANK_SWIFT`         | SWIFT/BIC                        |
| `BANK_IBAN`          | optional                         |

### 3.5 Resend (email)

| Var                | Notes                                |
|--------------------|--------------------------------------|
| `RESEND_API_KEY`   | `re_...`                             |
| `MAIL_FROM`        | `noreply@volcanoartscenter.rw` (must be a verified Resend domain) |
| `EMAIL_NOTIFICATIONS_ENABLED` | `true`                    |

### 3.6 Meta WhatsApp Cloud

| Var                        | Notes                                |
|----------------------------|--------------------------------------|
| `WHATSAPP_API_TOKEN`       | Permanent system-user token          |
| `WHATSAPP_PHONE_NUMBER_ID` | from Meta business manager           |
| `WHATSAPP_NOTIFICATIONS_ENABLED` | `true` (start `false` until templates are approved) |

### 3.7 Africa's Talking SMS

| Var                  | Notes                              |
|----------------------|------------------------------------|
| `AT_API_KEY`         | from AT dashboard                  |
| `AT_USERNAME`        | usually `sandbox` or your username |
| `AT_SENDER_ID`       | optional (alphanumeric sender ID)  |
| `SMS_NOTIFICATIONS_ENABLED` | `true`                      |

### 3.8 Partner JWT (RS256)

Generate one keypair, **never rotate without re-issuing tokens**.

```sh
openssl genpkey -algorithm RSA -out private.pem -pkeyopt rsa_keygen_bits:2048
openssl pkey -in private.pem -pubout -out public.pem
```

Set `PARTNER_JWT_PRIVATE_KEY` and `PARTNER_JWT_PUBLIC_KEY` to the **base64
body** of those PEMs (newlines and headers are stripped automatically).

| Var                          | Notes                                |
|------------------------------|--------------------------------------|
| `PARTNER_JWT_ISSUER`         | `https://api.volcanoartscenter.rw`   |
| `PARTNER_JWT_AUDIENCE`       | `volcano-partner-api`                |
| `PARTNER_JWT_KEY_ID`         | `partner-jwt-key-2026`               |
| `PARTNER_JWT_TTL_MINUTES`    | `60`                                 |
| `PARTNER_JWT_PRIVATE_KEY`    | (PEM body)                           |
| `PARTNER_JWT_PUBLIC_KEY`     | (PEM body)                           |

### 3.9 FedEx, S3

See `application.yml` for the full list — the same naming convention applies
(`FEDEX_BASE_URL`, `FEDEX_API_KEY`, `FEDEX_API_SECRET`, `FEDEX_ACCOUNT_NUMBER`,
`AWS_S3_REGION`, `AWS_S3_BUCKET_NAME`, `AWS_S3_ACCESS_KEY`, `AWS_S3_SECRET_KEY`).

---

## 4. Deploy steps

1. **Push to GitHub.** Railway auto-deploys on push to the connected branch.
2. **Create the service** from the repo, choose **Dockerfile** as the builder
   (Railway picks up the repo `Dockerfile` and `railway.json` automatically).
3. **Add Postgres + Redis add-ons.** Railway injects the URLs into the service
   env via reference variables.
4. **Set every var from §3** in the Service → Variables tab.
5. **First deploy** boots with `SPRING_PROFILES_ACTIVE=prod`, runs Flyway
   migrations V001 → V007 against the new database.
6. **Verify health:** `curl https://<railway-host>/actuator/health/liveness`
   should return `{"status":"UP"}`.
7. **Configure custom domain** under Service → Settings → Domains. Point your
   DNS `CNAME` to the railway-supplied target. Enable Railway-managed TLS.
8. **Register Stripe webhook** in dashboard pointing at
   `https://<your-domain>/api/v1/webhooks/stripe` — paste the new
   `whsec_...` into `STRIPE_WEBHOOK_SECRET` and redeploy.
9. **Register MoMo callback URL** = `MTN_MOMO_CALLBACK_URL`. (MoMo sets it
   per `requesttopay`, not globally — but make sure your service URL is
   reachable from MoMo's egress.)
10. **Smoke test from §6 below.**

---

## 5. PRD §18 launch checklist

| §  | Check                                                       | Where                                                |
|----|-------------------------------------------------------------|------------------------------------------------------|
| 1  | All migrations apply on a fresh database                    | `db/migration/V001..V007__*.sql`                     |
| 2  | Flyway `validate` passes on prod profile                    | `application-prod.yml` → `ddl-auto: validate`        |
| 3  | Health probes respond                                       | `/actuator/health/liveness`, `/actuator/health/readiness` |
| 4  | Stripe webhook signature verified                           | `WebhookController.handleStripeWebhook`              |
| 5  | MoMo webhook idempotent on `X-Reference-Id`                 | `WebhookProcessingService.processMomoCallback`       |
| 6  | Outbound email/SMS/WhatsApp persist-before-send + retries   | `OutboundChannelService`, `NotificationRetryScheduler` |
| 7  | Image upload presigned + 3 derivatives                      | `S3StorageService`, `ImagePipelineService`           |
| 8  | OWASP HTML sanitizer applied to blog content                | `HtmlSanitizerService` wired in `SuperAdminService`  |
| 9  | RS256 JWT issued for partner API + verified                 | `PartnerJwtService`, `JwtConfig`                     |
| 10 | RBAC enforced for `/api/v1/*`                               | `SecurityConfig`                                     |
| 11 | Rate limiting active                                        | `RateLimitInterceptor`                               |
| 12 | Idempotency keys on payment endpoints                       | `IdempotencyProperties`                              |
| 13 | Redis-backed sessions                                       | `spring.session.store-type: redis`                   |
| 14 | Audit aspect captures staff writes                          | `AuditAspect`, `@Audited` annotations                |
| 15 | Reference numbers atomic per scope+year                     | `ReferenceNumberService`                             |
| 16 | FX cache prevents thundering herd                           | `FxRateService` (lazy self-injected proxy + lock)    |
| 17 | Postgres FTS GIN index in place                             | `idx_products_search_vector`, `idx_experiences_search_vector` |
| 18 | Secure cookies + HSTS                                       | `application-prod.yml` + `SecurityConfig`            |
| 19 | Logs redact sensitive fields                                | `logback-spring.xml` (review patterns)               |
| 20 | Backup cadence configured                                   | `platform.monitoring.backup.cron`                    |

---

## 6. Smoke test sequence

Run after every deploy with a fresh production database.

```sh
BASE=https://api.volcanoartscenter.rw

# Liveness
curl -fsS $BASE/actuator/health/liveness

# Public catalog
curl -fsS "$BASE/api/v1/public/search?q=carving&limit=5" | jq

# FX cache
curl -fsS "$BASE/api/v1/public/fx/convert?amount=100&from=USD&to=EUR" | jq

# Anonymous donation (real Stripe test mode)
curl -fsS -X POST $BASE/api/v1/public/donations \
  -H "Content-Type: application/json" \
  -d '{"amount":50, "currency":"USD", "donorName":"Test Donor",
       "donorEmail":"test@example.com", "purpose":"GENERAL"}' | jq

# Partner JWT
curl -fsS -X POST $BASE/api/v1/partner/auth/token \
  -H "Content-Type: application/json" \
  -d '{"email":"partner@example.com","password":"..."}' | jq
```

---

## 7. Rollback

Railway keeps every deploy. Roll back via Service → Deployments → "Redeploy"
on the last green build. Database migrations are forward-only — design new
migrations to be backward-compatible (add columns first, drop later).

---

## 8. Observability

- Logs: Railway streams stdout. Structured pattern in `logback-spring.xml`.
- Metrics: `/actuator/prometheus` (locked to `SUPER_ADMIN` role; add a Railway
  internal scrape token if you wire a Prometheus instance).
- Health: Kubernetes-style liveness/readiness via `spring-boot-actuator`.

---

## 9. Performance targets (PRD §18.5)

| Endpoint                     | p95 budget | Smoke load        |
|------------------------------|-----------:|-------------------|
| `GET /api/v1/public/search`  | 200 ms     | 50 RPS sustained  |
| `POST /api/v1/client/checkout` | 600 ms   | 10 RPS sustained  |
| `POST /api/v1/client/bookings` | 600 ms   | 10 RPS sustained  |
| `GET /api/v1/public/fx/convert` | 100 ms (cached) | 100 RPS    |

Run the k6 scripts under `perf/` against a staging deploy before opening prod
traffic. See `perf/README.md`.
