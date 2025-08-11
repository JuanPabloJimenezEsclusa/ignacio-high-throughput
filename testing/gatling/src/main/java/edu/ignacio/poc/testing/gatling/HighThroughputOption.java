package edu.ignacio.poc.testing.gatling;

import java.util.Objects;

/**
 * The enum High throughput option.
 */
enum HighThroughputOption {
  IMPERATIVE("http://localhost:8888/imperative-throughput/smokes"),
  REACTIVE("http://localhost:9999/reactive-throughput/smokes");

  final String defaultURL;

  HighThroughputOption(final String defaultURL) {
    this.defaultURL = defaultURL;
  }

  /**
   * Of string.
   *
   * @param highThroughputOption the high throughput option
   * @param url the url
   * @return the string
   */
  static String of(final HighThroughputOption highThroughputOption, final String url) {
    return Objects.requireNonNullElse(url, highThroughputOption.defaultURL);
  }
}
