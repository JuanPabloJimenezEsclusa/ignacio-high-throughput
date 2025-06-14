package edu.ignacio.poc.imperativethroughput.config;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The type Metrics configuration.
 *
 * <p>Registers custom Micrometer metrics that complement the default Spring Boot
 * actuator metrics for a more targeted comparison between imperative and reactive
 * throughput:
 *
 * <ul>
 *   <li>{@code jvm.threads.virtual.active} — current number of live virtual threads,
 *       sampled from {@link ThreadMXBean}. Useful for observing how virtual thread
 *       count scales under load vs. the reactive module's fixed event-loop pool.</li>
 * </ul>
 *
 * <p>Per-endpoint request duration histograms ({@code http.request.duration}) are
 * registered directly in each controller to keep them close to the measured code.
 */
@Configuration
public class MetricsConfiguration {

  /**
   * Virtual thread active gauge.
   *
   * @param meterRegistry the meter registry
   * @return the gauge
   */
  @Bean
  public Gauge virtualThreadActiveGauge(final MeterRegistry meterRegistry) {
    final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    return Gauge.builder("jvm.threads.virtual.active",
        threadMXBean,
        this::countVirtualThreads)
      .description("Number of live virtual threads")
      .tag("module", "imperative")
      .register(meterRegistry);
  }

  private double countVirtualThreads(final ThreadMXBean bean) {
    return bean.getAllThreadIds().length;
  }
}
