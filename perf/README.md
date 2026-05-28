# Performance tests (k6)

Two profiles, in line with PRD §18.5 launch criteria.

## Install k6

```sh
brew install k6           # macOS
# or: https://k6.io/docs/getting-started/installation/
```

## Smoke (every deploy)

Single VU, 30 s, asserts liveness + happy-path public endpoints.

```sh
BASE_URL=https://api.volcanoartscenter.rw k6 run perf/smoke.js
```

Pass criteria:
- `http_req_failed` < 1%
- `http_req_duration` p95 < 500 ms

## Load (pre-launch)

Two scenarios in parallel:
- `/api/v1/public/search` — ramps to 50 RPS, holds 3 min
- `/api/v1/public/fx/convert` — ramps to 100 RPS, holds 3 min

```sh
BASE_URL=https://staging.volcanoartscenter.rw k6 run perf/load.js
```

Pass criteria:
- `search_latency` p95 < 200 ms
- `fx_latency` p95 < 100 ms (cache hit)
- `http_req_failed` < 1%

## Tuning notes

- Run against **staging**, not prod, the first time.
- The first FX request fans out to Frankfurter; ignore the warm-up p99 spike.
  Subsequent requests are cache hits served from Postgres.
- Search relies on the Postgres FTS GIN index added by V007. Verify with
  `\d+ products` in psql that `idx_products_search_vector` exists.
- If Railway scales to 0 between runs, expect cold-start latency on the
  first ~10 requests. Hold the test 30 s past warm-up before measuring p95.
