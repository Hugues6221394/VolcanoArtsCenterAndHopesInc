// k6 smoke test — verifies a fresh prod/staging deploy responds correctly.
// Run with:
//   BASE_URL=https://api.volcanoartscenter.rw k6 run perf/smoke.js

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  vus: 1,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

export default function () {
  const liveness = http.get(`${BASE}/actuator/health/liveness`);
  check(liveness, {
    'liveness 200': (r) => r.status === 200,
    'liveness UP': (r) => (r.json('status') === 'UP'),
  });

  const search = http.get(`${BASE}/api/v1/public/search?q=carving&limit=5`);
  check(search, {
    'search 200': (r) => r.status === 200,
    'search returns query': (r) => r.json('data.query') === 'carving',
  });

  const fx = http.get(`${BASE}/api/v1/public/fx/convert?amount=100&from=USD&to=EUR`);
  check(fx, {
    'fx 200': (r) => r.status === 200,
    'fx returns converted': (r) => typeof r.json('data.converted') !== 'undefined',
  });

  sleep(1);
}
