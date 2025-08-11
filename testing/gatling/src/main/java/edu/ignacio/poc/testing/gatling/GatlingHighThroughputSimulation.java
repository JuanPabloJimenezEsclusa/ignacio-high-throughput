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
import io.gatling.javaapi.http.HttpRequestActionBuilder;

/**
 * The type Gatling high throughput simulation.
 */
public class GatlingHighThroughputSimulation extends Simulation {

  private static final HttpRequestActionBuilder IMPERATIVE_CONFIG = setupGetConfiguration(
    IMPERATIVE, System.getenv("IMPERATIVE_URL"));
  private static final HttpRequestActionBuilder REACTIVE_CONFIG = setupGetConfiguration(
    REACTIVE, System.getenv("REACTIVE_URL"));

  private static HttpRequestActionBuilder setupGetConfiguration(final HighThroughputOption highThroughputOption,
                                                                final String url) {
    return http(highThroughputOption.name())
      .get(of(highThroughputOption, url))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .header("User-Agent", "gatling/performance-testing")
      .header("Cache-Control", "no-cache")
      .check(List.of(status().is(200)));
  }

  private static ScenarioBuilder buildImperativeScenario() {
    return CoreDsl.scenario("High Throughput Scenario")
      .exec(IMPERATIVE_CONFIG)
      .exec(REACTIVE_CONFIG);
  }

  private static OpenInjectionStep buildConstantRateStep() {
    final int totalUsers = 1_000;
    final long duration = 60L;
    return constantUsersPerSec(totalUsers).during(duration);
  }

  private static OpenInjectionStep buildRampRateStep() {
    final int totalUsers = 1_000;
    final double userRampUpPerInterval = 100;
    final double rampUpIntervalInSeconds = 300;
    final long duration = 120L;
    return rampUsersPerSec(userRampUpPerInterval / rampUpIntervalInSeconds)
      .to(totalUsers)
      .during(Duration.ofSeconds(duration))
      .randomized();
  }

  /**
   * Instantiates a new Gatling high throughput simulation.
   */
  public GatlingHighThroughputSimulation() {
    this.setUp(buildImperativeScenario()
      .injectOpen(buildConstantRateStep(), buildRampRateStep()));
  }
}
