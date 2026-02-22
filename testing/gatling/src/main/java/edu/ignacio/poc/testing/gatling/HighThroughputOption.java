package edu.ignacio.poc.testing.gatling;

import java.util.Objects;

/**
 * The enum High throughput option.
 */
enum HighThroughputOption {
  IMPERATIVE("http://localhost:8888/imperative-throughput"),
  REACTIVE("http://localhost:9999/reactive-throughput");

  final String defaultBaseUrl;

  HighThroughputOption(final String defaultBaseUrl) {
    this.defaultBaseUrl = defaultBaseUrl;
  }

  /**
   * Of string.
   *
   * @param highThroughputOption the high throughput option
   * @param baseUrl the base url override (from env), may be null
   * @return the resolved base URL
   */
  static String of(final HighThroughputOption highThroughputOption, final String baseUrl) {
    return Objects.requireNonNullElse(baseUrl, highThroughputOption.defaultBaseUrl);
  }
}
