package dev.jpje.testing.gatling;

import java.util.Objects;

enum HighThroughputOption {
  IMPERATIVE("http://localhost:8888/imperative-throughput"),
  REACTIVE("http://localhost:9999/reactive-throughput");

  final String defaultBaseUrl;

  HighThroughputOption(final String defaultBaseUrl) {
    this.defaultBaseUrl = defaultBaseUrl;
  }

  static String of(final HighThroughputOption highThroughputOption, final String baseUrl) {
    return Objects.requireNonNullElse(baseUrl, highThroughputOption.defaultBaseUrl);
  }
}
