package dev.jpje.imperativethroughput.config;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfiguration {

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
