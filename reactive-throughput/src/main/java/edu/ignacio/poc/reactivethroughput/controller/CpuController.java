package edu.ignacio.poc.reactivethroughput.controller;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import java.math.BigInteger;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * The type Cpu controller.
 *
 * <p>Demonstrates CPU-bound work dispatched via {@code Mono.fromCallable} +
 * {@code publishOn(boundedElastic())}. The event loop thread is released
 * immediately; computation runs on a bounded worker pool designed for
 * blocking/CPU-intensive tasks. Compare with the imperative counterpart
 * that uses a virtual-thread-per-task executor.
 */
@Configuration
public class CpuController extends AbstractReactiveController {

  private static final Logger log = LoggerFactory.getLogger(CpuController.class);
  private static final int FIBONACCI_N = 50;

  CpuController(@NonNull final MeterRegistry meterRegistry) {
    super(meterRegistry, "cpu");
  }

  /**
   * Cpu routes router function.
   *
   * @return the router function
   */
  @Bean
  public RouterFunction<ServerResponse> cpuRoutes() {
    return route(
      GET("/cpu").or(GET("/cpu/")),
      _ -> {
        final var sample = Timer.start();
        return Mono.fromCallable(this::fibonacci)
          .publishOn(Schedulers.boundedElastic())
          .flatMap(result -> {
            final var currentThread = Thread.currentThread();
            log.info("CPU reactive endpoint - fib({})={} - thread: {}", FIBONACCI_N, result, currentThread);
            sample.stop(this.timer);
            return this.okResponse()
              .bodyValue("OK:Reactive:CPU:fib(%d)=%d:%s".formatted(FIBONACCI_N, result, currentThread));
          })
          .doOnError(throwable -> log.error("Error in CPU reactive endpoint", throwable));
      });
  }

  private BigInteger fibonacci() {
    if (FIBONACCI_N <= 1) {
      return BigInteger.valueOf(FIBONACCI_N);
    }
    var a = BigInteger.ZERO;
    var b = BigInteger.ONE;
    for (int i = 2; i <= FIBONACCI_N; i++) {
      final var temp = a.add(b);
      a = b;
      b = temp;
    }
    return b;
  }
}
