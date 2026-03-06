import http from 'k6/http';
import { sleep } from 'k6';

export let options = {
  scenarios: {
    saturate: {
      executor: 'constant-vus',
      vus: 8,          // 8 concurrent users — more than our 5 thread pool
      duration: '30s',
    },
  },
  thresholds: {
    http_req_duration: ['p(99)<500'],  // will fail — intentional
    http_req_failed: ['rate<0.01'],    // will fail — intentional
  },
};

export default function () {
  let res = http.get('http://localhost:8080/slow');
  console.log(`Status: ${res.status} Duration: ${res.timings.duration}ms`);
}