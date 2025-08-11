import http from 'k6/http';
import { group, check, sleep } from 'k6';
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";
import { textSummary } from "https://jslib.k6.io/k6-summary/0.1.0/index.js";

export const options = {
  thresholds: {
    'http_req_duration{group:imperative}': [],
    'http_req_duration{group:reactive}': [],
    'http_reqs{group:imperative}': [],
    'http_reqs{group:reactive}': [],
    'http_req_waiting{group:imperative}': [],
    'http_req_waiting{group:reactive}': [],
  },
  scenarios: {
    high_throughput_test: {
      executor: 'ramping-arrival-rate',
      preAllocatedVUs: 7000,
      startRate: 1000,
      timeUnit: '5s',
      stages: [
        { target: 1000, duration: '1m' },
        { target: 4000, duration: '1m' },
        { target: 7000, duration: '1m' }
      ],
    }
  }
};

export default function () {
  highThroughputTest();
}

export function handleSummary(data) {
  return {
    "/result/summary.html": htmlReport(data),
    stdout: textSummary(data, { indent: "→", enableColors: true }),
  };
}

export function checkByImperativeGroup() {
  group('imperative', function() {
    const imperativeThroughput = __ENV.IMPERATIVE_THROUGHPUT_URL || 'http://imperative-throughput:8888/imperative-throughput/smokes';
    const params = {
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Cache-Control': 'no-cache'
      },
      responseType: 'text',
      tags: { group: 'imperative' }
    };

    const imperativeResult = http.get(imperativeThroughput, params);
    check(imperativeResult,
      { 'status was 200': (r) => r.status === 200,
        'body size < 100 bytes': (r) => r.body.length < 100,
        'body contains "OK"': (r) => r.body.includes('OK') });
  });
}

export function checkByReactiveGroup() {
  group('reactive', function() {
    const reactiveThroughput = __ENV.REACTIVE_THROUGHPUT_URL || 'http://reactive-throughput:9999/reactive-throughput/smokes';
    const params = {
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Cache-Control': 'no-cache'
      },
      responseType: 'text',
      tags: { group: 'reactive' }
    };

    const reactiveResult = http.get(reactiveThroughput, params);
    check(reactiveResult,
      { 'status was 200': (r) => r.status === 200,
        'body size < 100 bytes': (r) => r.body.length < 100,
        'body contains "OK"': (r) => r.body.includes('OK') });
  });

}

export function highThroughputTest() {
  checkByImperativeGroup();
  checkByReactiveGroup();
  sleep(1);
}
