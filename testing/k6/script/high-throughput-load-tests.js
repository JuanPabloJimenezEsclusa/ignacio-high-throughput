import http from 'k6/http';
import { group, check, sleep } from 'k6';
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";
import { textSummary } from "https://jslib.k6.io/k6-summary/0.1.0/index.js";

export const options = {
  thresholds: {
    'http_req_duration{group:imperative}': [],
    'http_req_duration{group:reactive}': [],
    'http_req_duration{group:imperative-smoke}': [],
    'http_req_duration{group:reactive-smoke}': [],
    'http_req_duration{group:imperative-io}': [],
    'http_req_duration{group:reactive-io}': [],
    'http_req_duration{group:imperative-cpu}': [],
    'http_req_duration{group:reactive-cpu}': [],
    'http_req_duration{group:imperative-aggregate}': [],
    'http_req_duration{group:reactive-aggregate}': [],
    'http_req_duration{group:imperative-resilience}': [],
    'http_req_duration{group:reactive-resilience}': [],
    'http_req_duration{group:reactive-stream}': [],
    'http_reqs{group:imperative}': [],
    'http_reqs{group:reactive}': [],
    'http_req_waiting{group:imperative}': [],
    'http_req_waiting{group:reactive}': [],
  },
  scenarios: {
    high_throughput_test: {
      executor: 'ramping-arrival-rate',
      preAllocatedVUs: 3000,
      startRate: 1000,
      timeUnit: '5s',
      stages: [
        { target: 1000, duration: '30s' },
        { target: 2000, duration: '30s' },
        { target: 3000, duration: '30s' }
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
  const baseUrl = __ENV.IMPERATIVE_THROUGHPUT_URL || 'http://imperative-throughput:8888/imperative-throughput';
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      'Cache-Control': 'no-cache'
    },
    responseType: 'text',
  };

  group('imperative-smoke', function() {
    const result = http.get(`${baseUrl}/smokes`, { ...params, tags: { group: 'imperative-smoke' } });
    check(result, {
      'status was 200': (r) => r.status === 200,
      'body size < 100 bytes': (r) => r.body.length < 100,
      'body contains "OK"': (r) => r.body.includes('OK'),
    });
  });

  group('imperative-io', function() {
    const result = http.get(`${baseUrl}/io`, { ...params, tags: { group: 'imperative-io' } });
    check(result, {
      'status was 200': (r) => r.status === 200,
      'body contains "OK:Imperative:IO"': (r) => r.body.includes('OK:Imperative:IO'),
    });
  });

  group('imperative-cpu', function() {
    const result = http.get(`${baseUrl}/cpu`, { ...params, tags: { group: 'imperative-cpu' } });
    check(result, {
      'status was 200': (r) => r.status === 200,
      'body contains "OK:Imperative:CPU"': (r) => r.body.includes('OK:Imperative:CPU'),
    });
  });

  group('imperative-aggregate', function() {
    const result = http.get(`${baseUrl}/aggregate`, { ...params, tags: { group: 'imperative-aggregate' } });
    check(result, {
      'status was 200': (r) => r.status === 200,
      'body contains "OK:Imperative:Aggregate"': (r) => r.body.includes('OK:Imperative:Aggregate'),
    });
  });

  group('imperative-resilience', function() {
    const result = http.get(`${baseUrl}/resilience`, { ...params, tags: { group: 'imperative-resilience' } });
    check(result, {
      'status was 200': (r) => r.status === 200,
      'body is not empty': (r) => r.body.length > 0,
    });
  });
}

export function checkByReactiveGroup() {
  const baseUrl = __ENV.REACTIVE_THROUGHPUT_URL || 'http://reactive-throughput:9999/reactive-throughput';
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      'Cache-Control': 'no-cache'
    },
    responseType: 'text',
  };

  group('reactive-smoke', function() {
    const result = http.get(`${baseUrl}/smokes`, { ...params, tags: { group: 'reactive-smoke' } });
    check(result, {
      'status was 200': (r) => r.status === 200,
      'body size < 100 bytes': (r) => r.body.length < 100,
      'body contains "OK"': (r) => r.body.includes('OK'),
    });
  });

  group('reactive-io', function() {
    const result = http.get(`${baseUrl}/io`, { ...params, tags: { group: 'reactive-io' } });
    check(result, {
      'status was 200': (r) => r.status === 200,
      'body contains "OK:Reactive:IO"': (r) => r.body.includes('OK:Reactive:IO'),
    });
  });

  group('reactive-cpu', function() {
    const result = http.get(`${baseUrl}/cpu`, { ...params, tags: { group: 'reactive-cpu' } });
    check(result, {
      'status was 200': (r) => r.status === 200,
      'body contains "OK:Reactive:CPU"': (r) => r.body.includes('OK:Reactive:CPU'),
    });
  });

  group('reactive-aggregate', function() {
    const result = http.get(`${baseUrl}/aggregate`, { ...params, tags: { group: 'reactive-aggregate' } });
    check(result, {
      'status was 200': (r) => r.status === 200,
      'body contains "OK:Reactive:Aggregate"': (r) => r.body.includes('OK:Reactive:Aggregate'),
    });
  });

  group('reactive-resilience', function() {
    const result = http.get(`${baseUrl}/resilience`, { ...params, tags: { group: 'reactive-resilience' } });
    check(result, {
      'status was 200': (r) => r.status === 200,
      'body is not empty': (r) => r.body.length > 0,
    });
  });

  group('reactive-stream', function() {
    const result = http.get(`${baseUrl}/stream`, {
      ...params,
      headers: { ...params.headers, 'Accept': 'text/event-stream' },
      tags: { group: 'reactive-stream' }
    });
    check(result, {
      'status was 200': (r) => r.status === 200,
      'body contains "event:"': (r) => r.body.includes('event:'),
    });
  });
}

export function highThroughputTest() {
  checkByImperativeGroup();
  checkByReactiveGroup();
  sleep(1);
}
