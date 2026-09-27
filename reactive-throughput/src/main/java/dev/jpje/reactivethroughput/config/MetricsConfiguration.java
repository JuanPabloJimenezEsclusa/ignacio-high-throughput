package dev.jpje.reactivethroughput.config;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jdk.management.VirtualThreadSchedulerMXBean;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfiguration {

  private static final Logger log = LoggerFactory.getLogger(MetricsConfiguration.class);
  private static final AtomicBoolean VIRTUAL_THREAD_SCHEDULER_WARNING_LOGGED = new AtomicBoolean();

  @Bean
  public Gauge platformThreadLiveGauge(@NonNull final MeterRegistry meterRegistry) {
    final var threadMXBean = ManagementFactory.getThreadMXBean();
    return Gauge.builder("jvm.threads.platform.live", threadMXBean, ThreadMXBean::getThreadCount)
      .description("Live platform threads, including virtual-thread carrier threads")
      .tag("module", "reactive")
      .register(meterRegistry);
  }

  @Bean
  public Gauge virtualThreadMountedGauge(@NonNull final MeterRegistry meterRegistry) {
    return this.registerVirtualThreadSchedulerGauge(
      meterRegistry,
      "jvm.threads.virtual.mounted",
      "Virtual threads currently mounted on a carrier thread",
      this::mountedVirtualThreads);
  }

  @Bean
  public Gauge virtualThreadQueuedGauge(@NonNull final MeterRegistry meterRegistry) {
    return this.registerVirtualThreadSchedulerGauge(
      meterRegistry,
      "jvm.threads.virtual.queued",
      "Virtual threads queued waiting for a carrier thread",
      this::queuedVirtualThreads);
  }

  @Bean
  public Gauge virtualThreadCarriersGauge(@NonNull final MeterRegistry meterRegistry) {
    return this.registerVirtualThreadSchedulerGauge(
      meterRegistry,
      "jvm.threads.virtual.carriers",
      "Virtual-thread scheduler carrier pool size",
      this::virtualThreadCarriers);
  }

  private Gauge registerVirtualThreadSchedulerGauge(final MeterRegistry meterRegistry,
                                                    final String name, final String description, final Supplier<Number> value) {
    if (virtualThreadSchedulerOrNull() == null) {
      return null;
    }
    return Gauge.builder(name, value)
      .description(description)
      .tag("module", "reactive")
      .register(meterRegistry);
  }

  private double mountedVirtualThreads() {
    final var scheduler = virtualThreadSchedulerOrNull();
    return scheduler == null ? Double.NaN : scheduler.getMountedVirtualThreadCount();
  }

  private double queuedVirtualThreads() {
    final var scheduler = virtualThreadSchedulerOrNull();
    return scheduler == null ? Double.NaN : scheduler.getQueuedVirtualThreadCount();
  }

  private double virtualThreadCarriers() {
    final var scheduler = virtualThreadSchedulerOrNull();
    return scheduler == null ? Double.NaN : scheduler.getPoolSize();
  }

  private static VirtualThreadSchedulerMXBean virtualThreadSchedulerOrNull() {
    try {
      return ManagementFactory.getPlatformMXBean(VirtualThreadSchedulerMXBean.class);
    } catch (final RuntimeException | LinkageError e) {
      if (VIRTUAL_THREAD_SCHEDULER_WARNING_LOGGED.compareAndSet(false, true)) {
        log.warn("VirtualThreadSchedulerMXBean is unavailable; registering only the platform thread gauge.", e);
      }
      return null;
    }
  }
}
