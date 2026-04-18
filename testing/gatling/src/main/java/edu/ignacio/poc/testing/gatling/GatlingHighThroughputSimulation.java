package edu.ignacio.poc.testing.gatling;

import static edu.ignacio.poc.testing.gatling.HighThroughputOption.IMPERATIVE;
import static edu.ignacio.poc.testing.gatling.HighThroughputOption.REACTIVE;
import static edu.ignacio.poc.testing.gatling.HighThroughputOption.of;
import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.rampUsersPerSec;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import java.time.Duration;
import java.util.List;

import io.gatling.javaapi.core.CoreDsl;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;

/**
 * The type Gatling high throughput simulation.
 *
 * <p>Covers all comparable endpoints on both modules so that performance results
 * are reproducible directly from source:
 * <ul>
 *   <li>{@code /smokes}    — baseline health / latency measurement</li>
 *   <li>{@code /cpu}       — CPU-bound work (fibonacci)</li>
 *   <li>{@code /io}        — I/O-bound work (blocking vs. non-blocking HTTP client)</li>
 *   <li>{@code /aggregate} — parallel fan-out (StructuredTaskScope vs. Flux.merge)</li>
 *   <li>{@code /resilience}— timeout + fallback behaviour</li>
 *   <li>{@code /stream}    — SSE backpressure (reactive module only)</li>
 * </ul>
 *
 * <p>Environment variable overrides (optional):
 * <ul>
 *   <li>{@code IMPERATIVE_BASE_URL} — default: {@code http://localhost:8888/imperative-throughput}</li>
 *   <li>{@code REACTIVE_BASE_URL}   — default: {@code http://localhost:9999/reactive-throughput}</li>
 * </ul>
 */
public class GatlingHighThroughputSimulation extends Simulation {

  private static final String IMPERATIVE_BASE = of(IMPERATIVE, System.getenv("IMPERATIVE_BASE_URL"));
  private static final String REACTIVE_BASE = of(REACTIVE, System.getenv("REACTIVE_BASE_URL"));
  private static final String APPLICATION_JSON = "application/json";

  // ── Imperative HTTP protocol config ──────────────────────────────────────
  private static final HttpProtocolBuilder IMPERATIVE_PROTOCOL = http
    .baseUrl(IMPERATIVE_BASE)
    .acceptHeader(APPLICATION_JSON)
    .contentTypeHeader(APPLICATION_JSON)
    .userAgentHeader("gatling/performance-testing")
    .header("Cache-Control", "no-cache");

  // ── Reactive HTTP protocol config ─────────────────────────────────────────
  private static final HttpProtocolBuilder REACTIVE_PROTOCOL = http
    .baseUrl(REACTIVE_BASE)
    .acceptHeader(APPLICATION_JSON)
    .contentTypeHeader(APPLICATION_JSON)
    .userAgentHeader("gatling/performance-testing")
    .header("Cache-Control", "no-cache");

  // ── Endpoint request builders ─────────────────────────────────────────────
  private static HttpRequestActionBuilder get200(final String name, final String path) {
    return http(name).get(path).check(List.of(status().is(200)));
  }

  // ── Imperative scenario ───────────────────────────────────────────────────
  private static ScenarioBuilder buildImperativeScenario() {
    return CoreDsl.scenario("Imperative - All Endpoints")
      .exec(get200("imperative /smokes", "/smokes"))
      .exec(get200("imperative /cpu", "/cpu"))
      .exec(get200("imperative /io", "/io"))
      .exec(get200("imperative /aggregate", "/aggregate"))
      .exec(get200("imperative /resilience", "/resilience"));
  }

  // ── Reactive scenario ─────────────────────────────────────────────────────
  private static ScenarioBuilder buildReactiveScenario() {
    return CoreDsl.scenario("Reactive - All Endpoints")
      .exec(get200("reactive /smokes", "/smokes"))
      .exec(get200("reactive /cpu", "/cpu"))
      .exec(get200("reactive /io", "/io"))
      .exec(get200("reactive /aggregate", "/aggregate"))
      .exec(get200("reactive /resilience", "/resilience"))
      // /stream is SSE — just verify the connection opens (first chunk arrives)
      .exec(http("reactive /stream").get("/stream")
        .header("Accept", "text/event-stream")
        .check(status().is(200)));
  }

  // ── Injection profiles ────────────────────────────────────────────────────
  private static OpenInjectionStep buildConstantRateStep() {
    final int totalUsers = 100;
    final long duration = 30L;
    return constantUsersPerSec(totalUsers).during(duration);
  }

  private static OpenInjectionStep buildRampRateStep() {
    final int totalUsers = 100;
    final double userRampUpPerInterval = 200;
    final double rampUpIntervalInSeconds = 500;
    final long duration = 60L;
    return rampUsersPerSec(userRampUpPerInterval / rampUpIntervalInSeconds)
      .to(totalUsers)
      .during(Duration.ofSeconds(duration))
      .randomized();
  }

  /**
   * Instantiates a new Gatling high throughput simulation.
   */
  public GatlingHighThroughputSimulation() {
    this.setUp(
      buildImperativeScenario()
        .injectOpen(buildConstantRateStep(), buildRampRateStep())
        .protocols(IMPERATIVE_PROTOCOL),
      buildReactiveScenario()
        .injectOpen(buildConstantRateStep(), buildRampRateStep())
        .protocols(REACTIVE_PROTOCOL)
    );
  }
}
