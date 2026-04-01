import http from 'k6/http';
import { sleep } from 'k6';
// const URL = "https://lab-2-2-554533316554.asia-southeast1.run.app";
const URL = "http://localhost:8090";
// const prefix = "broken";
const prefix = "fixed";

export let options = {
  scenarios: {
    slow_flood: {
      executor: 'shared-iterations',
      vus: 8,
      iterations: 8,
      startTime: '0s',
      exec: 'slowRequest',
    },
    fast_requests: {
      executor: 'constant-vus',
      vus: 10,
      duration: '15s',
      startTime: '1s',  // starts 1s after slow flood fires
      exec: 'fastRequest',
    }
  }
};

export function slowRequest() {
  http.get(`${URL}/lab22/${prefix}/slow`);
}

export function fastRequest() {
  let res = http.get(`${URL}/lab22/${prefix}/fast`);
  console.log(`fast response: ${res.status} - ${res.timings.duration}ms`);
  sleep(1);
}
