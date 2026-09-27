import http from 'k6/http';
import { group, check, sleep } from 'k6';
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";
import { textSummary } from "https://jslib.k6.io/k6-summary/0.1.0/index.js";

export const options = {
  thresholds: {
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
    http_req_duration: ['p(99)<60000'],
    'http_req_duration{group:imperative-smoke}': ['p(95)<5000'],
    'http_req_duration{group:reactive-smoke}': ['p(95)<5000'],
  },
  scenarios: {
    high_throughput_test: {
      executor: 'ramping-arrival-rate',
      // Raised from 3000: each iteration now also probes the /resilience fallback, which holds a
      // VU for the full stub delay (~1.5 s) plus the in-flight task, roughly doubling VU-seconds
      // per iteration. Without headroom the top stage would drop iterations.
      preAllocatedVUs: 6000,
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
  // Defaults to the legacy flat path so behaviour is unchanged when K6_SUMMARY_HTML is not set.
  const htmlPath = __ENV.K6_SUMMARY_HTML || "/result/summary.html";
  return {
    [htmlPath]: htmlReport(data),
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
    const result = http.get(`${baseUrl}/smokes`, { ...params, tags: { group: 'imperative-smoke', impl: 'imperative' } });
    check(result, {
      'status was 200': (r) => r.status === 200,
      'body size < 100 bytes': (r) => r.body.length < 100,
      'body contains "OK"': (r) => r.body.includes('OK'),
    });
  });

  group('imperative-io', function() {
    const result = http.get(`${baseUrl}/io`, { ...params, tags: { group: 'imperative-io', impl: 'imperative' } });
    check(result, {
      'status was 200': (r) => r.status === 200,
      'body contains "OK:Imperative:IO"': (r) => r.body.includes('OK:Imperative:IO'),
    });
  });

  group('imperative-cpu', function() {
    const result = http.get(`${baseUrl}/cpu`, { ...params, tags: { group: 'imperative-cpu', impl: 'imperative' } });
    check(result, {
      'status was 200': (r) => r.status === 200,
      'body contains "OK:Imperative:CPU"': (r) => r.body.includes('OK:Imperative:CPU'),
    });
  });

  group('imperative-aggregate', function() {
    const result = http.get(`${baseUrl}/aggregate`, { ...params, tags: { group: 'imperative-aggregate', impl: 'imperative' } });
    check(result, {
      'status was 200': (r) => r.status === 200,
      'body contains "OK:Imperative:Aggregate"': (r) => r.body.includes('OK:Imperative:Aggregate'),
    });
  });

  group('imperative-resilience', function() {
    const result = http.get(`${baseUrl}/resilience`, { ...params, tags: { group: 'imperative-resilience', impl: 'imperative' } });
    check(result, {
      'status was 200': (r) => r.status === 200,
      'body is not empty': (r) => r.body.length > 0,
    });
  });

  group('imperative-resilience-fallback', function() {
    const result = http.get(`${baseUrl}/resilience?delayMs=1500`, { ...params, tags: { group: 'imperative-resilience-fallback', impl: 'imperative' } });
    check(result, {
      'status was 200': (r) => r.status === 200,
      'body contains "FALLBACK:Imperative:Resilience"': (r) => r.body.includes('FALLBACK:Imperative:Resilience'),
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
    const result = http.get(`${baseUrl}/smokes`, { ...params, tags: { group: 'reactive-smoke', impl: 'reactive' } });
    check(result, {
      'status was 200': (r) => r.status === 200,
      'body size < 100 bytes': (r) => r.body.length < 100,
      'body contains "OK"': (r) => r.body.includes('OK'),
    });
  });

  group('reactive-io', function() {
    const result = http.get(`${baseUrl}/io`, { ...params, tags: { group: 'reactive-io', impl: 'reactive' } });
    check(result, {
      'status was 200': (r) => r.status === 200,
      'body contains "OK:Reactive:IO"': (r) => r.body.includes('OK:Reactive:IO'),
    });
  });

  group('reactive-cpu', function() {
    const result = http.get(`${baseUrl}/cpu`, { ...params, tags: { group: 'reactive-cpu', impl: 'reactive' } });
    check(result, {
      'status was 200': (r) => r.status === 200,
      'body contains "OK:Reactive:CPU"': (r) => r.body.includes('OK:Reactive:CPU'),
    });
  });

  group('reactive-aggregate', function() {
    const result = http.get(`${baseUrl}/aggregate`, { ...params, tags: { group: 'reactive-aggregate', impl: 'reactive' } });
    check(result, {
      'status was 200': (r) => r.status === 200,
      'body contains "OK:Reactive:Aggregate"': (r) => r.body.includes('OK:Reactive:Aggregate'),
    });
  });

  group('reactive-resilience', function() {
    const result = http.get(`${baseUrl}/resilience`, { ...params, tags: { group: 'reactive-resilience', impl: 'reactive' } });
    check(result, {
      'status was 200': (r) => r.status === 200,
      'body is not empty': (r) => r.body.length > 0,
    });
  });

  group('reactive-resilience-fallback', function() {
    const result = http.get(`${baseUrl}/resilience?delayMs=1500`, { ...params, tags: { group: 'reactive-resilience-fallback', impl: 'reactive' } });
    check(result, {
      'status was 200': (r) => r.status === 200,
      'body contains "FALLBACK:Reactive:Resilience"': (r) => r.body.includes('FALLBACK:Reactive:Resilience'),
    });
  });

  group('reactive-stream', function() {
    const result = http.get(`${baseUrl}/stream`, {
      ...params,
      headers: { ...params.headers, 'Accept': 'text/event-stream' },
      tags: { group: 'reactive-stream', impl: 'reactive' }
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
