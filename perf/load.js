// k6 load test — public read paths under sustained traffic.
// Run with:
//   BASE_URL=https://staging.volcanoartscenter.rw k6 run perf/load.js
//
// Tracks the PRD §18.5 budgets:
//   /api/v1/public/search       p95 < 200 ms @ 50 RPS sustained
//   /api/v1/public/fx/convert   p95 < 100 ms (cache hit) @ 100 RPS sustained

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const searchTrend = new Trend('search_latency', true);
const fxTrend = new Trend('fx_latency', true);

export const options = {
  scenarios: {
    search: {
      executor: 'ramping-arrival-rate',
      exec: 'searchScenario',
      startRate: 5,
      timeUnit: '1s',
      preAllocatedVUs: 30,
      maxVUs: 100,
      stages: [
        { target: 25, duration: '30s' },
        { target: 50, duration: '1m' },
        { target: 50, duration: '3m' },
        { target: 0,  duration: '30s' },
      ],
    },
    fx: {
      executor: 'ramping-arrival-rate',
      exec: 'fxScenario',
      startRate: 10,
      timeUnit: '1s',
      preAllocatedVUs: 30,
      maxVUs: 150,
      stages: [
        { target: 50,  duration: '30s' },
        { target: 100, duration: '1m' },
        { target: 100, duration: '3m' },
        { target: 0,   duration: '30s' },
      ],
    },
  },
  thresholds: {
    'http_req_failed':                    ['rate<0.01'],
    'search_latency':                     ['p(95)<200'],
    'fx_latency':                         ['p(95)<100'],
  },
};

const queries = ['carving', 'wood', 'rwanda', 'cultural tour', 'painting'];

export function searchScenario() {
  const q = queries[Math.floor(Math.random() * queries.length)];
  const r = http.get(`${BASE}/api/v1/public/search?q=${encodeURIComponent(q)}&limit=10`);
  searchTrend.add(r.timings.duration);
  check(r, { 'search 200': (res) => res.status === 200 });
}

export function fxScenario() {
  const r = http.get(`${BASE}/api/v1/public/fx/convert?amount=100&from=USD&to=EUR`);
  fxTrend.add(r.timings.duration);
  check(r, { 'fx 200': (res) => res.status === 200 });
}
