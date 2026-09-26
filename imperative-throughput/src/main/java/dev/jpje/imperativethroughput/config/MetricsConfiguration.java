package dev.jpje.imperativethroughput.config;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jdk.management.VirtualThreadSchedulerMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The type Metrics configuration.
 *
 * <p>Registers custom Micrometer gauges that complement the default Spring Boot actuator
 * metrics for a more targeted comparison between imperative and reactive throughput:
 *
 * <ul>
 *   <li>{@code jvm.threads.platform.live} — live platform threads reported by
 *       {@link ThreadMXBean#getThreadCount()}. Virtual-thread carrier threads are platform
 *       threads, so they are included in this number.</li>
 *   <li>{@code jvm.threads.virtual.mounted} — virtual threads currently mounted on a carrier
 *       thread, from {@link VirtualThreadSchedulerMXBean#getMountedVirtualThreadCount()}.</li>
 *   <li>{@code jvm.threads.virtual.queued} — virtual threads queued waiting for a carrier
 *       thread, from {@link VirtualThreadSchedulerMXBean#getQueuedVirtualThreadCount()}.</li>
 *   <li>{@code jvm.threads.virtual.carriers} — carrier pool size of the virtual-thread
 *       scheduler, from {@link VirtualThreadSchedulerMXBean#getPoolSize()}.</li>
 * </ul>
 */
@Configuration
public class MetricsConfiguration {

  private static final Logger log = LoggerFactory.getLogger(MetricsConfiguration.class);
  private static final AtomicBoolean VIRTUAL_THREAD_SCHEDULER_WARNING_LOGGED = new AtomicBoolean();

  @Bean
  public Gauge platformThreadLiveGauge(final MeterRegistry meterRegistry) {
    final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    return Gauge.builder("jvm.threads.platform.live", threadMXBean, ThreadMXBean::getThreadCount)
      .description("Live platform threads, including virtual-thread carrier threads")
      .tag("module", "imperative")
      .register(meterRegistry);
  }

  @Bean
  public Gauge virtualThreadMountedGauge(final MeterRegistry meterRegistry) {
    return this.registerVirtualThreadSchedulerGauge(
      meterRegistry,
      "jvm.threads.virtual.mounted",
      "Virtual threads currently mounted on a carrier thread",
      this::mountedVirtualThreads);
  }

  @Bean
  public Gauge virtualThreadQueuedGauge(final MeterRegistry meterRegistry) {
    return this.registerVirtualThreadSchedulerGauge(
      meterRegistry,
      "jvm.threads.virtual.queued",
      "Virtual threads queued waiting for a carrier thread",
      this::queuedVirtualThreads);
  }

  @Bean
  public Gauge virtualThreadCarriersGauge(final MeterRegistry meterRegistry) {
    return this.registerVirtualThreadSchedulerGauge(
      meterRegistry,
      "jvm.threads.virtual.carriers",
      "Virtual-thread scheduler carrier pool size",
      this::virtualThreadCarriers);
  }

  private Gauge registerVirtualThreadSchedulerGauge(
    final MeterRegistry meterRegistry,
    final String name,
    final String description,
    final Supplier<Number> value
  ) {
    if (virtualThreadSchedulerOrNull() == null) {
      return null;
    }
    return Gauge.builder(name, value)
      .description(description)
      .tag("module", "imperative")
      .register(meterRegistry);
  }

  private double mountedVirtualThreads() {
    final VirtualThreadSchedulerMXBean scheduler = virtualThreadSchedulerOrNull();
    return scheduler == null ? Double.NaN : scheduler.getMountedVirtualThreadCount();
  }

  private double queuedVirtualThreads() {
    final VirtualThreadSchedulerMXBean scheduler = virtualThreadSchedulerOrNull();
    return scheduler == null ? Double.NaN : scheduler.getQueuedVirtualThreadCount();
  }

  private double virtualThreadCarriers() {
    final VirtualThreadSchedulerMXBean scheduler = virtualThreadSchedulerOrNull();
    return scheduler == null ? Double.NaN : scheduler.getPoolSize();
  }

  private static VirtualThreadSchedulerMXBean virtualThreadSchedulerOrNull() {
    try {
      return ManagementFactory.getPlatformMXBean(VirtualThreadSchedulerMXBean.class);
    } catch (final RuntimeException | LinkageError e) {
      if (VIRTUAL_THREAD_SCHEDULER_WARNING_LOGGED.compareAndSet(false, true)) {
        log.warn("VirtualThreadSchedulerMXBean is unavailable; registering only the platform thread gauge. Cause: {}",
          e.toString());
      }
      return null;
    }
  }
}
