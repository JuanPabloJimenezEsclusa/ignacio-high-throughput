package dev.jpje.imperativethroughput.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Metrics Configuration Test")
class MetricsConfigurationTest {

  private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
  private final MetricsConfiguration configuration = new MetricsConfiguration();

  @Test
  @DisplayName("Registers the honest platform live-thread gauge with a positive value")
  void registersPlatformLiveGauge() {
    final Gauge gauge = this.configuration.platformThreadLiveGauge(this.registry);

    assertThat(gauge).as("platform live gauge must be registered").isNotNull();
    assertThat(this.registry.find("jvm.threads.platform.live").tag("module", "imperative").gauge())
      .as("platform live gauge must be in registry with imperative tag").isNotNull();
    assertThat(gauge.value()).as("platform live gauge value must be positive").isGreaterThan(0.0);
    assertThat(gauge.getId().getDescription())
      .as("platform live gauge description must match")
      .isEqualTo("Live platform threads, including virtual-thread carrier threads");
  }

  @Test
  @DisplayName("Does not register the removed jvm.threads.virtual.active gauge")
  void removedVirtualActiveGaugeIsAbsent() {
    this.configuration.platformThreadLiveGauge(this.registry);

    assertThat(this.registry.find("jvm.threads.virtual.active").gauge()).isNull();
  }

  @Test
  @DisplayName("Registers the virtual-thread mounted gauge with a non-negative value")
  void registersVirtualThreadMountedGauge() {
    final var freshRegistry = new SimpleMeterRegistry();
    this.configuration.virtualThreadMountedGauge(freshRegistry);

    final Gauge mounted = freshRegistry.find("jvm.threads.virtual.mounted").tag("module", "imperative").gauge();
    assertThat(mounted).as("mounted gauge must be registered").isNotNull();
    assertThat(mounted.value()).as("mounted gauge value must be non-negative").isGreaterThanOrEqualTo(0.0);
    assertThat(mounted.getId().getDescription())
      .as("mounted gauge description must match")
      .isEqualTo("Virtual threads currently mounted on a carrier thread");
  }

  @Test
  @DisplayName("Registers the virtual-thread queued gauge with a non-negative value")
  void registersVirtualThreadQueuedGauge() {
    final var freshRegistry = new SimpleMeterRegistry();
    this.configuration.virtualThreadQueuedGauge(freshRegistry);

    final Gauge queued = freshRegistry.find("jvm.threads.virtual.queued").tag("module", "imperative").gauge();
    assertThat(queued).as("queued gauge must be registered").isNotNull();
    assertThat(queued.value()).as("queued gauge value must be non-negative").isGreaterThanOrEqualTo(0.0);
  }

  @Test
  @DisplayName("Registers the virtual-thread carriers gauge with a non-negative value")
  void registersVirtualThreadCarriersGauge() {
    final var freshRegistry = new SimpleMeterRegistry();
    this.configuration.virtualThreadCarriersGauge(freshRegistry);

    final Gauge carriers = freshRegistry.find("jvm.threads.virtual.carriers").tag("module", "imperative").gauge();
    assertThat(carriers).as("carriers gauge must be registered").isNotNull();
    assertThat(carriers.value()).as("carriers gauge value must be non-negative").isGreaterThanOrEqualTo(0.0);
  }
}
